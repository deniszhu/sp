package com.soulinfo.jms.book.listener;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.qiniu.api.auth.digest.Mac;
import com.qiniu.api.rs.PutPolicy;
import com.soulinfo.framework.util.service.GlobalConfigService;
import com.soulinfo.jms.book.producer.BookUpdateMessageJms;
import com.soulinfo.yearbook.dao.BookDao;
import com.soulinfo.yearbook.dao.page.PageDao;
import com.soulinfo.yearbook.dao.page.SectionDao;
import com.soulinfo.yearbook.model.Book;
import com.soulinfo.yearbook.model.page.Page;
import com.soulinfo.yearbook.model.page.Section;
import com.soulinfo.yearbook.util.Constant;

@Component("wkThreadImplThumbnailProcessor")
public class WkThreadImplThumbnailProcessor {
	private static BlockingQueue<String> msgQueue = new LinkedBlockingDeque<String>(1000);

	private boolean isRun = false;
	
	private int wkThreadNum = 10;

	@Autowired
	private PageDao pageDao;
	
	@Autowired
	private SectionDao sectionDao;
	
	@Autowired
	private BookDao bookDao;
	
	@Autowired
	private Mac mac;
	
	@Autowired
	private PutPolicy bookPutPolicy;
	
	@Autowired
	@Qualifier("appProperties")
	private Properties appProperties;
	
	@Autowired
	protected GlobalConfigService gcService;
	
	private String baseDomain = "http://localhost/";
	
	private String domain = "http://yearbook-book.qiniudn.com/";
	
	private String pageLayoutUrl = "/export/page/" ;
	
	private String pageThumbnailPath = "/mnt/sure/yearbook/page/";

	
	public void doProcessor(BookUpdateMessageJms bookUpdateMessageJms) {
		runWKProcessThread();
		
		String msgType = (String)bookUpdateMessageJms.getType();
		switch (msgType) {
		case "Page":
			doPageProcess(bookUpdateMessageJms);
			break;
		case "Section":
			doSectionProcess(bookUpdateMessageJms);
			break;
		case "Book":
			doBookProcess(bookUpdateMessageJms);
			break;
		case "TPL":
			doBookChangeTpl(bookUpdateMessageJms);
		default:
			break;
		}

	}
	
	private boolean doBookChangeTpl(BookUpdateMessageJms msg) {
		try {
			List<String> pageList = msg.getPageList();
			for (String pageId : pageList) {
				msgQueue.offer(pageId);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return true;
	}
	
	private boolean doPageProcess(BookUpdateMessageJms msg) {
		try {
			String pageId = (String) msg.getPageId();
			Page page = pageDao.get(pageId);
			if (pageId != null) {
				msgQueue.offer(page.getId());
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return true;
	}
	
	private boolean doSectionProcess(BookUpdateMessageJms msg) {
		try {
			String sectionId =  (String) msg.getSectionId();
			Section section = sectionDao.get(sectionId);
			if (section != null) {
				List<Page> pls = section.getPages();
				for (Page p : pls) {
					if (Page.TYPE_EMPTY.equals(p.getType()))
						continue;
					msgQueue.offer(p.getId());
					break;
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return true;
	}
	
	private boolean doBookProcess(BookUpdateMessageJms msg) {
		try {
			String bookId = (String) msg.getBookId();
			String event = (String) msg.getEvent();
			Book book = bookDao.get(bookId);
			if (book != null) {
				
				List<Section> indexS = sectionDao.getBookSectionByType(bookId, Section.TYPE_PINDEX);
				
				//目录更新
				if (event != null && (event.equals("addPage") || event.equals("delPage") ||
						event.equals("addSection") || event.equals("delSection") || event.equals("changeSectionConfig"))) {
					List<Section> cataS = sectionDao.getBookSectionByType(bookId, Section.TYPE_CATALOGUE);
					for (Section section : cataS) {
						List<Page> ps = section.getPages();
						for (Page page : ps) {
							if (Page.TYPE_EMPTY.equals(page.getType()))
								continue;
							msgQueue.offer(page.getId());
						}
					}
					for (Section section : indexS) {
						List<Page> ps = section.getPages();
						for (Page page : ps) {
							if (Page.TYPE_EMPTY.equals(page.getType()))
								continue;
							msgQueue.offer(page.getId());
						}
					}
				}
				
				//通信录更新
				if (event != null && (event.equals("delUser") || event.equals("addUser") ||
						event.equals("createBook") || event.equals("rebuildIndex"))) {
					for (Section section : indexS) {
						List<Page> ps = section.getPages();
						for (Page page : ps) {
							if (Page.TYPE_EMPTY.equals(page.getType()))
								continue;
							msgQueue.offer(page.getId());
						}
					}
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return false;
	}
		
	private void initThread(WKProcessThread thread){
		String appId = appProperties.getProperty("appId", Constant.appId);
		String bD = gcService.getConfig("baseConfig", appId, "baseUrl", false);
		if (bD != null)
			baseDomain = bD;	
		
		String plUrl = gcService.getConfig("wkConfig", appId, "pageLayoutUrl", false);
		if (plUrl != null)
			pageLayoutUrl = plUrl;	
		
		String ptUrl = gcService.getConfig("wkConfig", appId, "pageThumbnailPath", false);
		if (ptUrl != null)
			pageThumbnailPath = ptUrl;
		
		String qiniuDomain = gcService.getConfig("qiniu", appId, "qiniu.book.domain", false);
		if (qiniuDomain != null)
			domain = qiniuDomain;
		
		thread.setDomain(domain);
		thread.setBaseDomain(baseDomain);
		thread.setPageLayoutUrl(pageLayoutUrl);
		thread.setPageThumbnailPath(pageThumbnailPath);
		thread.setPageDao(pageDao);
		thread.setBookPutPolicy(bookPutPolicy);
		thread.setMac(mac);
	}
	
	private void runWKProcessThread() {
		if (!isRun) {
			ExecutorService service = Executors.newCachedThreadPool();
			
			for (int i = 0; i < wkThreadNum; i++) {
				WKProcessThread wks = new WKProcessThread(msgQueue);
				initThread(wks);
				service.execute(wks);
			}
			isRun = true;
		}
	}

	public int getWkThreadNum() {
		return wkThreadNum;
	}

	public void setWkThreadNum(int wkThreadNum) {
		this.wkThreadNum = wkThreadNum;
	}
}
