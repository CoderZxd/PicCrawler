package com.test.pic.crawler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**   
 * @Title: PicClawlerForSingleUrl.java 
 *
 * @Package com.test.pic.crawler 
 *
 * @Description: TODO(用一句话描述该文件做什么) 
 *
 * @author zouxd   
 *
 * @date 2018年1月12日 下午10:15:38 
 *
 * @version V1.0   
 *
 */
public class PicClawlerForSingleUrl {

	static String pathString = "G:/test/pic/";
	/**
	 * @throws IOException  
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
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		String url="https://www.aitaotu.com/meinv/25754.html";
		long begin = System.currentTimeMillis();
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
	}

}
