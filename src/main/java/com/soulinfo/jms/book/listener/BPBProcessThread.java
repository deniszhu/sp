package com.soulinfo.jms.book.listener;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qiniu.api.auth.AuthException;
import com.qiniu.api.auth.digest.Mac;
import com.qiniu.api.io.IoApi;
import com.qiniu.api.io.PutExtra;
import com.qiniu.api.io.PutRet;
import com.qiniu.api.rs.PutPolicy;
import com.soulinfo.framework.exception.DAOException;
import com.soulinfo.yearbook.dao.BookDao;
import com.soulinfo.yearbook.dao.PrintBookDao;
import com.soulinfo.yearbook.dao.PrintPageDao;
import com.soulinfo.yearbook.dao.page.PageDao;
import com.soulinfo.yearbook.model.Book;
import com.soulinfo.yearbook.model.page.Page;
import com.soulinfo.yearbook.model.print.PrintBook;
import com.soulinfo.yearbook.model.print.PrintPage;
import com.soulinfo.yearbook.util.WkUtil;

public class BPBProcessThread implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(BPBProcessThread.class); 
	
	private Mac mac;

	private PutPolicy bookPutPolicy;
	
	private PrintPageDao printPageDao;
	
	private PrintBookDao printBookDao;
	
	private BookDao bookDao;
	
	private PageDao pageDao;
	
	private String qiniuDomain = "http://yearbook-book.qiniudn.com/";
	
	private String pageLayoutUrl = "/export/page/" ;
	
	private String bookThumbnailPath = "/mnt/sure/yearbook/printBook/";
	
	private String baseDomain = "http://localhost/";
	
	private BlockingQueue<Map<String, Object>> queue = null;
	
	private Lock lock = new ReentrantLock(); 

	public BPBProcessThread(BlockingQueue<Map<String, Object>> queue) {
		this.queue = queue;	
	}
	
	public BPBProcessThread() {
		
	}
	
	@Override
	public void run() {
		Map<String, Object> msg = null;  
        try { 
       	 while (true) {
       		 msg = queue.take();
       		 doProcessOnePage(msg); 
       	 }
        } catch (InterruptedException e) {  
            e.printStackTrace();  
        } catch (DAOException e) {
			e.printStackTrace();
		}  
		
	}
	
	
	private boolean doProcessOnePage(Map<String, Object> msg) throws DAOException {
		
		PrintPage printPage = (PrintPage) msg.get("printPage");
		PrintBook pb = (PrintBook) msg.get("printBook");
		
		String pageId = printPage.getPageId();
		int pagination = printPage.getPagination();	
		Page page = pageDao.get(pageId);
		
		String localFile = doLocalThumbnail(page, pb, pagination);
		if (localFile != null) {
			return doQiniuThumbnail(page, pb, printPage, localFile);
		}
		
		return false;
	}
	
	private String doLocalThumbnail(Page page, PrintBook pb, int pagination ) {
		logger.debug("生成BPB本地图片...开始!");
		
		String bookId = pb.getBookId();
		String pbId = pb.getId();
		String pageId = page.getId();
		
		String pageHtml = baseDomain + pageLayoutUrl + pageId  + "/pdf";
		
		String pageImg = bookThumbnailPath + bookId + "/"+ pbId + "/" + pagination + ".jpeg";
		
		String pageDirPath = bookThumbnailPath + bookId + "/" + pbId;
		File pageDir = new File(pageDirPath);
		if (!pageDir.exists()) {
			pageDir.mkdirs();
		}
		String localFile = WkUtil.html2Image(pageHtml, pageImg, 2480, 1748);
		
		logger.debug("LocalFile=" + localFile);
		logger.debug("生成BPB本地图片...结束!");
		return localFile;
	}

	public boolean doQiniuThumbnail(Page page, PrintBook pb, PrintPage printPage, String localFile) {
		
		logger.debug("上传BPB本地图片到七牛服务器...开始!");
		
		String pageId = page.getId();
		String bookId = pb.getBookId();
		String pbId = pb.getId();
		int pagination = printPage.getPagination();
		
		PutExtra extra = new PutExtra();
        String key =  "/printBook/" + bookId + "/" + pbId + "/" + pagination + ".jpeg";
        
        String uptoken;
		try {
			PutPolicy pp = new PutPolicy(bookPutPolicy.scope + ":" + key);
			uptoken = pp.token(mac);
	        PutRet ret = IoApi.putFile(uptoken, key, localFile, extra);
	         	
	        if (ret.getStatusCode() == 200) {
	        	printPage.setPageImage(qiniuDomain + ret.getKey());
	        	
	        	logger.debug("上传图片 ok, pageId:" + pageId + ", ps:" + pagination);
	        	printPageDao.saveOrUpdate(printPage);
	        } else {
	        	logger.error("上传图片 失败, ret:" + ret.getStatusCode() + 
	        			", pageId:" + pageId + ", ps:" + pagination);
	        }
	        
	        logger.debug("上传BPB本地图片到七牛服务器...结束!");
	        
	        Collection<PrintPage> pps = printPageDao.getListByAttr("bookId", pb.getId());
	        for (PrintPage prPage : pps) {
				if (prPage.getPageImage() == null || prPage.getPageImage() == "")
					return true;
			}
	        
	       
	        if (pb != null && lock.tryLock()) {
	        	 try {
	        		pb.setStatus(PrintBook.STATUS_OK);
	 	        	printBookDao.update(pb);
	 	        	Book book = bookDao.get(bookId);
	 	        	book.setStatus(Book.BOOK_STATE_SUBMIT);
	 	        	bookDao.update(book);
	 			} catch (Exception e) {
	 				
	 			}
	        	
	        	lock.unlock();
	        	
	        	logger.debug("生成打印书册完成, book:" + pb.getBookId() +
	        			", pBook:" + pb.getId());
	        }  
	        
		} catch (AuthException | JSONException e) {
			e.printStackTrace();
		} catch (DAOException e) {
			e.printStackTrace();
		} 
		return true;
	}

	public Mac getMac() {
		return mac;
	}

	public void setMac(Mac mac) {
		this.mac = mac;
	}

	public PutPolicy getBookPutPolicy() {
		return bookPutPolicy;
	}

	public void setBookPutPolicy(PutPolicy bookPutPolicy) {
		this.bookPutPolicy = bookPutPolicy;
	}

	public PrintPageDao getPrintPageDao() {
		return printPageDao;
	}

	public void setPrintPageDao(PrintPageDao printPageDao) {
		this.printPageDao = printPageDao;
	}

	public PrintBookDao getPrintBookDao() {
		return printBookDao;
	}

	public void setPrintBookDao(PrintBookDao printBookDao) {
		this.printBookDao = printBookDao;
	}

	public BookDao getBookDao() {
		return bookDao;
	}

	public void setBookDao(BookDao bookDao) {
		this.bookDao = bookDao;
	}

	public PageDao getPageDao() {
		return pageDao;
	}

	public void setPageDao(PageDao pageDao) {
		this.pageDao = pageDao;
	}

	public String getQiniuDomain() {
		return qiniuDomain;
	}

	public void setQiniuDomain(String qiniuDomain) {
		this.qiniuDomain = qiniuDomain;
	}

	public String getPageLayoutUrl() {
		return pageLayoutUrl;
	}

	public void setPageLayoutUrl(String pageLayoutUrl) {
		this.pageLayoutUrl = pageLayoutUrl;
	}

	public String getBookThumbnailPath() {
		return bookThumbnailPath;
	}

	public void setBookThumbnailPath(String bookThumbnailPath) {
		this.bookThumbnailPath = bookThumbnailPath;
	}

	public String getBaseDomain() {
		return baseDomain;
	}

	public void setBaseDomain(String baseDomain) {
		this.baseDomain = baseDomain;
	}

	public BlockingQueue<Map<String, Object>> getQueue() {
		return queue;
	}

	public void setQueue(BlockingQueue<Map<String, Object>> queue) {
		this.queue = queue;
	}

	public Lock getLock() {
		return lock;
	}

	public void setLock(Lock lock) {
		this.lock = lock;
	}
}
