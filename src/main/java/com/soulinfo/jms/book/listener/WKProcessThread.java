package com.soulinfo.jms.book.listener;

import java.io.File;
import java.util.List;
import java.util.concurrent.BlockingQueue;

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
import com.soulinfo.yearbook.dao.page.PageDao;
import com.soulinfo.yearbook.model.page.Page;
import com.soulinfo.yearbook.model.page.Section;
import com.soulinfo.yearbook.util.WkUtil;

public class WKProcessThread implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(WKProcessThread.class); 
	
	/* 书页的地址 */
	private String pageLayoutUrl = "/export/page/";
	/* 缩略图存放地址 */
	private String pageThumbnailPath = "/mnt/sure/yearbook/page/";
	/* 服务器基地址 */
	private String baseDomain = "http://localhost/";
	
	/* 七牛空间的地址 */
	private String domain = "http://yearbook-book.qiniudn.com/";
	
	/* 消息队列 */
	private BlockingQueue<String> recvQueue = null;


	private PageDao pageDao;
	
	/* 七牛的MAC */
	private Mac mac;
	
	private PutPolicy bookPutPolicy;
	
	public WKProcessThread() {
		
	}
	
	public WKProcessThread(BlockingQueue<String> recvQueue) {
		this.recvQueue = recvQueue;	
	}
	
	@Override
	public void run() {
		String pageId = null;  
         try { 
        	 logger.info("RUN WKProcessThread .....");
  
        	 while (true) {
        		pageId = recvQueue.take();
        		Page page = pageDao.get(pageId);
			
        		if (page != null) {
        			if (Page.TYPE_FLYLEAF.equals(page.getType()) ||
        					Page.TYPE_MONTAGE.equals(page.getType()) ||
        					Page.TYPE_PANO.equals(page.getType())) {
        				Section section = page.getSection();
        				List<Page> pls = section.getPages();
        				for (Page p : pls) {
        					if (Page.TYPE_EMPTY.equals(p.getType()))
        							continue;
        					doProcessOnePage(p.getId());
						}
        			} else {
        				doProcessOnePage(pageId);	
        			}
        		}

        	 }
         } catch (InterruptedException | DAOException e ) {  
             e.printStackTrace();  
         }  
     } 
	
	private boolean doProcessOnePage(String pageId) throws DAOException {
		Page page = pageDao.get(pageId);
		String localFile = doLocalThumbnail(page);
		
		if (localFile != null) {
			return doQiniuThumbnail(page, localFile);
		}
		
		return false;
	}
	
	private String doLocalThumbnail(Page page) throws DAOException {
		logger.debug("处理页面生成本地图片...开始!");
		String localFile = null;
		String pageId = page.getId();
		
		String pageHtml = baseDomain + pageLayoutUrl + pageId + "/pdf";
		String pageImg = pageThumbnailPath + pageId + "/thumbnail.jpeg";
		
		String pageDirPath = pageThumbnailPath + pageId;
		File pageDir = new File(pageDirPath);
		if (!pageDir.exists()) {
			pageDir.mkdirs();
		}
		localFile = WkUtil.html2Image(pageHtml, pageImg, 2480, 1748);
		logger.debug("处理页面生成本地图片...结束!");
		
		return localFile;
		
	}
	
	private boolean doQiniuThumbnail(Page page, String localFile) {
		
		logger.debug("上传本地文件到七牛服务器...开始");
		String pageId = page.getId();
		
		try {	
			int status = page.getStatus();
			
			PutExtra extra = new PutExtra();
	        String key = "/page/" + pageId + "/thumbnail";
	        String uptoken;
		
			PutPolicy pp = new PutPolicy(bookPutPolicy.scope + ":" + key);
			uptoken = pp.token(mac);
	        PutRet ret = IoApi.putFile(uptoken, key, localFile, extra);
	        
	        if (ret.getStatusCode() == 200) {
		        if (page != null) {
		        	String thumbnail = domain + ret.getKey();
		        	if (status == Page.STATUS_SAVE_THUMB) {
						page.setStatus(Page.STATUS_EDIT);
		        	} else if (status == Page.STATUS_SUB_THUMB) {
		        		page.setStatus(Page.STATUS_SUBMIT);
		        	}
		        	
		        	page.setThumbnail(thumbnail);
		        	page.setThumbnailPos(page.getTempletePos());
		        }      
	        } else {
	        	logger.error("Do qiniu thumbnail Error! ret = %d ", ret.getStatusCode());
	        	page.setStatus(Page.STATUS_ERROR);
	        }
	        pageDao.update(page);
	        logger.debug("上传本地文件到七牛服务器...结束!");
		}catch (AuthException | JSONException | DAOException  e) {
			e.printStackTrace();
		}
		
		return true;
	}
	
	public String getPageLayoutUrl() {
		return pageLayoutUrl;
	}

	public void setPageLayoutUrl(String pageLayoutUrl) {
		this.pageLayoutUrl = pageLayoutUrl;
	}

	public String getPageThumbnailPath() {
		return pageThumbnailPath;
	}

	public void setPageThumbnailPath(String pageThumbnailPath) {
		this.pageThumbnailPath = pageThumbnailPath;
	}

	public String getBaseDomain() {
		return baseDomain;
	}

	public void setBaseDomain(String baseDomain) {
		this.baseDomain = baseDomain;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public BlockingQueue<String> getRecvQueue() {
		return recvQueue;
	}

	public void setRecvQueue(BlockingQueue<String> recvQueue) {
		this.recvQueue = recvQueue;
	}

	public PageDao getPageDao() {
		return pageDao;
	}

	public void setPageDao(PageDao pageDao) {
		this.pageDao = pageDao;
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
}
