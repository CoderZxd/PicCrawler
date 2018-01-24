package com.test.pic.crawler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;




/**   
 * @Title: PicCrawler.java 
 *
 * @Package com.test.pic.crawler 
 *
 * @Description: TODO(用一句话描述该文件做什么) 
 *
 * @author zouxd   
 *
 * @date 2018年1月9日 下午11:22:41 
 *
 * @version V1.0   
 *
 */
public class PicCrawler implements Runnable{
	private static String pathString = "D:/test/pic/";
	//存储真正的爬取页面
	static BlockingQueue<String> urlBlockingQueue = new ArrayBlockingQueue<String>(1000);
	static int threadNum = 20;
//	public PicCrawler(String url){
//		this.url = url;
//	}

	/** 
	 * @Title: main 
	 *
	 * @Description: TODO(这里用一句话描述这个方法的作用) 
	 *
	 * @param @param args    设定文件 
	 *
	 * @return void    返回类型 
	 *
	 * @throws 
	 *
	 */
	public static void main(String[] args) {
		String homeurlString = "https://www.aitaotu.com";
		String tagPageUrl = "https://www.aitaotu.com/tag/";
		//Tag标签的完整路径
		Set<String> tagFullHrefSet = new HashSet<String>(16);
		//想要爬去哪些tag，如果为空，则全部爬取;否则只配置对应的tag
		String[] crawlerTagArray = {"风景"};
		List<String> crawlerTagList = Arrays.asList(crawlerTagArray);
		try {
			//1.获取想要的tag完整的url
			Document tagListDocument = Jsoup.connect(tagPageUrl).get();
			Elements tagsListDivElements = tagListDocument.getElementsByClass("tags_list");
			for(Element element:tagsListDivElements){
				Elements aElements = element.getElementsByTag("a");
				for(Element a:aElements){
					if(crawlerTagList.size() == 0 || crawlerTagList.contains(a.text())){
						String tagUrlString = homeurlString+a.attr("href");
						//https://www.aitaotu.com/tag/feitun.html
						tagUrlString = tagUrlString.substring(0, tagUrlString.lastIndexOf("."))+"/1.html";
						tagFullHrefSet.add(tagUrlString);
					}
				}
			}
			//2.获取图片链接页面地址，分页爬取
			for(String tagUrl:tagFullHrefSet){
				String tempTagUrlString = tagUrl;
				int currentPageNum = 1;
				while(true){
					try{
						Document imagePageDocument = Jsoup.connect(tempTagUrlString).get();
						Elements imageListElements = imagePageDocument.getElementsByClass("Pli-litpic");
						if(imageListElements.size() == 0){
							break;
						}
						for(Element image:imageListElements){
							urlBlockingQueue.offer(homeurlString+image.attr("href"));
						}
						//https://www.aitaotu.com/tag/juru/1.html
						tempTagUrlString = tempTagUrlString.substring(0, tempTagUrlString.lastIndexOf("/")+1)+(++currentPageNum)+".html";
					}catch(Exception e){
						break;
					}
				}
			}
			ScheduledExecutorService excutor = new ScheduledThreadPoolExecutor(threadNum,new BasicThreadFactory.Builder().namingPattern("my-crawler-thread-%d").daemon(false).build());
			for(int i=0;i<threadNum;i++){
//				excutor.schedule(new PicCrawler(urlArray[i]), 1, TimeUnit.SECONDS);
//				excutor.execute(new PicCrawler(urlArray[i]));
				excutor.submit(new PicCrawler());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Override
	public void run() {
		while (true) {
			try {
				long begin = System.currentTimeMillis();
				String url = urlBlockingQueue.poll();
				if(null != url){
					Document doc = Jsoup.connect(url).get();
					Elements titleElements =doc.select("#photos > h1");
					if(null != titleElements && null != titleElements.get(0)){
						Set<String> imgSrcSet = new HashSet<String>(16);
						Element titleElement = titleElements.get(0);
						String foldNameString = titleElement.text();
						String[] nameArray = foldNameString.split("\\(");
						foldNameString = nameArray[0];
						nameArray = nameArray[nameArray.length-1].split("/");
						int totalPaggs = Integer.parseInt(nameArray[1].replace(")", ""));
						for(int i=1;i<=totalPaggs;i++){
							String urlTemp = url.replace(".html", "_"+i+".html");
							Document docTemp = Jsoup.connect(urlTemp).get();
							Element element = docTemp.getElementById("big-pic");
							Elements imgElements = element.getElementsByTag("img");
							for(Element imgElement:imgElements){
								imgSrcSet.add(imgElement.attr("src"));
							}
						}
						if(imgSrcSet.size()>0){
							for(String imgSrc:imgSrcSet){
								// 构造URL    
								URL imgurl = new URL(imgSrc);    
								// 打开连接    
								URLConnection con = imgurl.openConnection();    
								//设置请求超时为5s    
								con.setConnectTimeout(10*1000);    
								// 输入流    
								InputStream is = con.getInputStream();    
								// 500k的数据缓冲    
								byte[] bs = new byte[1024*500];    
								// 读取到的数据长度    
								int len;    
								// 输出的文件流    
								File sf=new File(pathString+"\\"+foldNameString);    
								if(!sf.exists()){    
									sf.mkdirs();    
								}
								String filename = imgSrc.split("/")[imgSrc.split("/").length-1];
								OutputStream os = new FileOutputStream(sf.getPath()+"\\"+filename);    
								// 开始读取    
								while ((len = is.read(bs)) != -1) {    
									os.write(bs, 0, len);    
								}    
								// 完毕，关闭所有链接    
								os.close();    
								is.close();  
								System.out.println(imgSrc+"下载完成!!!");
							}
						}
						long end = System.currentTimeMillis();
						System.out.println("================================================================");
						System.out.println(Thread.currentThread().getName()+"******************已全部下载完成，用时："+((end-begin)/1000)+"S");
					}
				}else{
					System.out.println("========================BlockingQueue已空，已全部抓取完成!=======================");
				}
			} catch (Exception e) {
				System.out.println("========================抓取异常=======================");
				System.exit(0);
			}
		}
	}
}
