package com.soulinfo.jms.book.listener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.qiniu.api.auth.digest.Mac;
import com.qiniu.api.rs.PutPolicy;
import com.soulinfo.framework.util.service.GlobalConfigService;
import com.soulinfo.jms.book.producer.BookUpdateMessageJms;
import com.soulinfo.yearbook.dao.BookDao;
import com.soulinfo.yearbook.dao.PrintBookDao;
import com.soulinfo.yearbook.dao.PrintPageDao;
import com.soulinfo.yearbook.dao.page.PageDao;
import com.soulinfo.yearbook.model.print.PrintBook;
import com.soulinfo.yearbook.model.print.PrintPage;
import com.soulinfo.yearbook.util.Constant;

@Component("bpbThreadImplThumbnailProcessor")
public class BPBThreadImplThumbnailProcessor {

	@Value("")
	private int bpbThreadNum = 10;
	
	private int bpbQueueNum = 1000;
	
	@Autowired
	protected GlobalConfigService gcService;
	
	@Autowired
	@Qualifier("appProperties")
	private Properties appProperties;
	
	@Autowired
	private Mac mac;
	
	@Autowired
	@Qualifier("bookPutPolicy")
	private PutPolicy bookPutPolicy;
	
	@Autowired
	private BookDao bookDao;

	@Autowired
	protected PrintBookDao printBookDao;
	
	@Autowired
	protected PageDao pageDao;
	
	@Autowired
	protected PrintPageDao printPageDao;
	
	private static BlockingQueue<Map<String, Object>> msgQueue = 
			new LinkedBlockingDeque<Map<String, Object>>(1000);
	
	private static Lock lock = new ReentrantLock();

	private String qiniuDomain = "http://yearbook-book.qiniudn.com/";
	private String pageLayoutUrl = "/export/page/" ;
	private String bookThumbnailPath = "/mnt/sure/yearbook/printBook/";
	
	private String baseDomain = "http://localhost/";

	private boolean isRun = false;
	
	public BPBThreadImplThumbnailProcessor() {
		super();
		//runBPBProcessThread();
	}

	public void doProcessor(BookUpdateMessageJms msg) {
		runBPBProcessThread();
		
		String msgType = (String) msg.getType();
		switch (msgType) {
		case "PrintBook":
			doPrintBookProcess(msg);
			break;

		default:
			break;
		}
	}
	
	private boolean doPrintBookProcess(BookUpdateMessageJms msg) {
		
		try {
			String pbId = (String) msg.getPrintBookId();
			PrintBook pb = printBookDao.get(pbId);
			
			List<PrintPage> pp = pb.getPrintPages();
			for (PrintPage printPage : pp) {
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("printPage", printPage);
				map.put("printBook", pb);
				
				msgQueue.offer(map);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		return false;
	}

	private void runBPBProcessThread() {
		if (!isRun) {
			ExecutorService service = Executors.newCachedThreadPool();
			msgQueue = new LinkedBlockingDeque<Map<String, Object>>(bpbQueueNum);
			for (int i = 0; i < bpbThreadNum; i++) {
				BPBProcessThread wks = new BPBProcessThread(msgQueue);
				initBPBThread(wks);
				service.execute(wks);
			}
			isRun = true;
		}
		
	}
	
	private void initBPBThread(BPBProcessThread thread){
		String appId = appProperties.getProperty("appId", Constant.appId);
		String bD = gcService.getConfig("baseConfig", appId, "baseUrl", true);
		if (bD != null)
			baseDomain = bD;
		
		String plUrl = gcService.getConfig("wkConfig", appId, "pageLayoutUrl", false);
		if (plUrl != null)
			pageLayoutUrl = plUrl;
		
		String ptUrl = gcService.getConfig("wkConfig", appId, "bookThumbnailPath", false);
		if (ptUrl != null)
			bookThumbnailPath = ptUrl;
		
		String qndomain = gcService.getConfig("qiniu", appId, "qiniu.book.domain", false);
		if (qndomain != null)
			qiniuDomain = qndomain;
		
		thread.setBaseDomain(baseDomain);
		thread.setPageLayoutUrl(pageLayoutUrl);
		thread.setBookThumbnailPath(bookThumbnailPath);
		thread.setQiniuDomain(qiniuDomain);
		
		thread.setBookDao(bookDao);
		thread.setPageDao(pageDao);
		thread.setPrintBookDao(printBookDao);
		thread.setPrintPageDao(printPageDao);
		
		thread.setMac(mac);
		thread.setBookPutPolicy(bookPutPolicy);
		
		thread.setLock(lock);
		
	}

	public int getBpbThreadNum() {
		return bpbThreadNum;
	}

	public void setBpbThreadNum(int bpbThreadNum) {
		this.bpbThreadNum = bpbThreadNum;
	}

	public int getBpbQueueNum() {
		return bpbQueueNum;
	}

	public void setBpbQueueNum(int bpbQueueNum) {
		this.bpbQueueNum = bpbQueueNum;
	}
}
