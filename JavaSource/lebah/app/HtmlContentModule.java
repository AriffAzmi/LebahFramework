/* ************************************************************************
LEBAH PORTAL FRAMEWORK
Copyright (C) 2007  Shamsul Bahrin

 * ************************************************************************ */

/* ************************************************************************
LEBAH PORTAL FRAMEWORK
Copyright (C) 2007  Shamsul Bahrin

 * ************************************************************************ */

package lebah.app;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpSession;

import lebah.db.DbException;
import lebah.portal.HtmlModuleData;
import lebah.portal.action.Command;
import lebah.portal.action.LebahModule;
import lebah.portal.db.RegisterModule;
import lebah.util.FilesRepositoryModule.Resource;

/**
 * @author Shamsul Bahrin Abd Mutalib
 * @version 1.01
 */
public class HtmlContentModule extends LebahModule implements lebah.portal.HtmlContainer {

	private String strUrl = "";
	private String moduleTitle = "";
	private String path = "vtl/url";
	private String userId = "";
	
	public void preProcess() {
		HttpSession session = request.getSession();
		userId = (String) session.getAttribute("_portal_login");
		context.put("userId", userId);
		String isLogin = (String) session.getAttribute("_portal_islogin");
		context.put("isLogin", "true".equals(isLogin) ? Boolean.TRUE : Boolean.FALSE);	
		context.put("url", strUrl);
		context.put("contentUrl", strUrl);
	}


	@Override
	public String start() {
		HttpSession session = request.getSession();

		String submit = getParam("command");
		String formname = getParam(getId());
		if ( !formname.equals(getId()) ) submit = "";		
		if ( "changeProperties".equals(submit)) {
			strUrl = getParam("url");
			moduleTitle = getParam("moduleTitle");
			try {
				RegisterModule.updateHtmlLocation(moduleId, strUrl);
			} catch (DbException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else if ( "deleteModule".equals(submit)) {
			String tab_id = (String) session.getAttribute("_tab_id");
			try {
				RegisterModule.deleteUserModule(tab_id, moduleId, userId);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		try {
			displayContent(session);
		} catch ( Exception e ) {
			context.put("stringbuffer", e.getMessage());
		}

		//realease for gc
		context.put("stringbuffer", null);
		return path + "/urlcontent.vm";
	}

	//set the url
	public void setUrl(String strUrl) {
		this.strUrl = strUrl;
	}


	private void displayContent(HttpSession session) throws Exception {

		URL url = null;
		try {
			String http = request.getRequestURL().toString().substring(0, request.getRequestURL().toString().indexOf("://") + 3);
			if (strUrl.indexOf(http)== -1 ) {
				String hostURL = (String) session.getAttribute("_portal_server");
				String appName = (String) session.getAttribute("_portal_appname");
				if ( strUrl.charAt(0) == '/' ) {
					strUrl = http + hostURL + strUrl;
				} else {
					strUrl = http + hostURL + "/" + appName + "/" + strUrl;
				}
			}		
			url = new URL(strUrl);

			//---strUrl must be HTML file, (.htm or .html) and must not contains <FRAMSET
			if ( strUrl.length() < 8 ) throw new Exception("Invalid URL");
			//take the last 3 chars or the last 4 chars of the strUrl	
			String last4 = strUrl.substring(strUrl.length()-4);
			String last5 = strUrl.substring(strUrl.length()-5);
			/*
			if ( !last4.equals(".htm") && !last5.equals(".html") )
				throw new Exception("The link must be to an html file!");
			 */
			//in the url, look for last "/"
			int last_bslash = strUrl.lastIndexOf("/");
			//and get this url
			String url2 = strUrl.substring(0, last_bslash);
			InputStream content = (InputStream) url.getContent();	  
			StringBuffer buf = new StringBuffer();
			int ch = 0, prevchar = 0;
			while ( (ch = content.read() ) != - 1) {
				buf.append((char) ch);
			}
			//remove <style>
			String str = buf.toString().toLowerCase();
			if ( (str.indexOf("<style") > -1) && (str.indexOf("</style>") > -1) ) {
				String str1 = buf.substring(0, str.indexOf("<style"));
				String str2 = buf.substring(str.indexOf("</style>") + "</style>".length());
				buf = new StringBuffer(str1).append(str2);
			}
			//look for <img src
			//replacetoFullURL(buf, "<img src", url2);
			//replacetoFullURL(buf, "<embed src", url2);
			replacetoFullURL(buf, "<a href", url2);
			//replacetoFullURL(buf, "<img src", url2);
			replacetoFullURL(buf, "src=", url2);
			context.put("stringbuffer", buf);
		} catch( MalformedURLException e1) {
			throw new Exception("MalformedURLException: " + e1.getMessage());
		} catch ( IOException e2 ) {
			throw new Exception("IOException: " + e2.getMessage());
		}
	}

	private void replacetoFullURL(StringBuffer contentBuffer, String str_tag, String url2) {
		String str = contentBuffer.toString();
		int pos_start = 0;
		while (true) { //infinite loop!!
			int tag_open = str.indexOf(str_tag, pos_start);
			if ( tag_open  == -1 ) break; //get out of this loop when no more img src
			//start from here, look for ">"
			StringBuffer sb = new StringBuffer();
			int cnt = 0;
			for ( cnt = tag_open; cnt < str.length(); cnt++ ) {
				sb.append(str.charAt(cnt));
				if ( str.charAt(cnt) == '>' ) break;
			}
			int tag_close = cnt;
			//analyze
			//find first "="
			str = sb.toString();
			int first_eq = str.indexOf("=");
			//take left hand side of "=" and trim it
			String eq_left = str.substring(0, first_eq).trim();

			//add target="_new" before the '>' in the eq_right, but only for http
			if ( "<a ".equals(eq_left.substring(0, "<a ".length()) ) ) {
				eq_left = "<a target=\"_new\" " + eq_left.substring("<a ".length());
			}			
			//take right hand side of "=" and trim it
			String eq_right = str.substring(first_eq + 1).trim();
			//get the first "/" after http://, this url is root
			String url_root = url2;
			String http = request.getRequestURL().toString().substring(0, request.getRequestURL().toString().indexOf("://") + 3);
			int first_bslash = url2.indexOf("/", http.length() + 1);

			if ( first_bslash > 0 ) url_root = url2.substring(0, first_bslash);
			if ( eq_right.length() > http.length()+1) {
				if ( eq_right.charAt(0) == '\"' ) {
					//must not start with http:// and mailto:
					if ( !http.equals(eq_right.substring(1, http.length()+1))
							&& !"mailto:".equals(eq_right.substring(1, "mailto:".length()+1))) {

						//eg: "/image/home.gif"
						if ( eq_right.startsWith("\"/") ) {
							eq_right = "\"" + url_root + eq_right.substring(1);
						} else {
							eq_right = "\"" + url2 + "/" + eq_right.substring(1);
						}
					}
				} else {
					if ( !http.equals(eq_right.substring(0, http.length()+1))
							&& !"mailto:".equals(eq_right.substring(0, "mailto:".length()+1))) {

						//eg: "/image/home.gif"
						if ( eq_right.startsWith("\"/") ) {
							eq_right = url_root + eq_right.substring(0);
						} else {
							eq_right = url2 + "/" + eq_right;
						}						

					}
				}
			}

			//construct back the whole string
			str = eq_left + "=" + eq_right;
			pos_start = tag_open + str.length();
			//now replace in the buf
			contentBuffer.replace(tag_open, tag_close+1, str);	
			str = contentBuffer.toString();
		}
	}

	void changeProperties(String userId, String id, String url, String moduleTitle) throws Exception {
		HtmlModuleData.update(userId, id, url, moduleTitle);
	}

	private String readContent(String fileName) {
		try {
			StringBuffer sb = new StringBuffer();
			BufferedReader in = new BufferedReader(new FileReader(fileName));
			String str;
			while ((str = in.readLine()) != null) {
				sb.append(str);
			}
			in.close();
			return sb.toString();
		} catch (Exception e) {
			return "";
		}

	}

	private void writeContent(String content, String fileName) {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
			out.write(content);
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	@Command("editContent")
	public String editContent() throws Exception {
		String contentUrl = getParam("contentUrl");
		context.put("contentUrl", contentUrl);
		
		String root = Resource.getROOT();
		String dirName = Resource.getPATH();
		String fileName = dirName + contentUrl.substring(root.length() + 2);
		
		String content = readContent(fileName);
		context.put("content", content);
         
		return path + "/urlcontent_edit.vm";
	}
	
	@Command("saveContent")
	public String saveContent() throws Exception {
		String formname = getParam("formname");
		if ( !"".equals(formname)) {
			String contentUrl = getParam("contentUrl_" + formname);
			context.put("contentUrl", contentUrl);
			String root = Resource.getROOT();
			String dirName = Resource.getPATH();
			String fileName = dirName + contentUrl.substring(root.length() + 2);
			String content = getParam("editor_" + formname);
			writeContent(content, fileName);
			context.put("contentSaved", true);
		} else {
			context.remove("contentSaved");
		}
		return path + "/urlcontent_saved.vm";
	}
	
	@Command("showUpdatedContent")
	public String showUpdatedContent() throws Exception {
		System.out.println("showUpdatedContent");
		displayContent(request.getSession());
		return path + "/urlcontent_display.vm";
	}

}
