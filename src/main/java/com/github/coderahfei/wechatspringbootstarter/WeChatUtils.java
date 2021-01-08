package com.github.coderahfei.wechatspringbootstarter;

import com.github.coderahfei.wechatspringbootstarter.config.WechatConfig;
import com.github.coderahfei.wechatspringbootstarter.model.*;
import com.github.coderahfei.wechatspringbootstarter.utils.HttpsUtil;
import com.github.coderahfei.wechatspringbootstarter.utils.JackJsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 微信工具类
 *
 * @author yang [yiixuan@163.com]
 */
public class WeChatUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(WechatConfig.class);
	private static WechatConfig wechatConfig;
	
	public WeChatUtils(WechatConfig wechatConfig) {
		WeChatUtils.wechatConfig = wechatConfig;
	}
	
	/**
	 * 根据code获取网页授权的access_token
	 *
	 * @param code 参数
	 * @return 返回值
	 */
	public static AuthToken getWebAccessToken(String code) {
		AuthToken authToken = null;
		StringBuilder json = new StringBuilder();
		try {
			String urlStr = wechatConfig.getUrl().getWebAccessToken() + "appid=" + wechatConfig.getConfig().getAppid() + "&secret=" + wechatConfig.getConfig().getAppsecret() + "&code=" + code + "&grant_type=authorization_code";
			LOGGER.info("-------------------------------------------------------------------------------------");
			LOGGER.info("获取网页授权的access_token的url【" + urlStr + "】");
			URL url = new URL(urlStr);
			URLConnection uc = url.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				json.append(inputLine);
			}
			in.close();
			LOGGER.info("-------------------------------------------------------------------------------------");
			LOGGER.info("获取网页授权的access_token是【" + json.toString() + "】");
			//将json字符串转成javaBean
			ObjectMapper objectMapper = new ObjectMapper();
			authToken = objectMapper.readValue(json.toString(), AuthToken.class);
		} catch (Exception e) {
			LOGGER.error("微信工具类:根据授权code获取access_token异常");
			e.printStackTrace();
		}
		return authToken;
	}
	
	/**
	 * 根据微信网页授权的access_token获取用户的基本信息
	 *
	 * @param accessToken 参数
	 * @return 返回值
	 */
	public static UserInfoDto getUserInfoByWechatLogin(AuthToken accessToken) {
		UserInfoDto userInfoDto = null;
		StringBuilder json = new StringBuilder();
		try {
			String urlStr = wechatConfig.getUrl().getUserInfo() + "?access_token=" + accessToken.getAccess_token() + "&openid=" + accessToken.getOpenid();
			LOGGER.info("-------------------------------------------------------------------------------------");
			LOGGER.info("根据微信网页授权的access_token获取用户的基本信息【" + urlStr + "】");
			URL url = new URL(urlStr);
			URLConnection uc = url.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				json.append(inputLine);
			}
			in.close();
			LOGGER.info("-------------------------------------------------------------------------------------");
			LOGGER.info("获取用户的基本信息是【" + json.toString() + "】");
			//将json字符串转成javaBean
			ObjectMapper objectMapper = new ObjectMapper();
			userInfoDto = objectMapper.readValue(json.toString(), UserInfoDto.class);
		} catch (Exception e) {
			LOGGER.error("微信工具类:根据微信网页授权的access_token获取用户的基本信息异常");
			e.printStackTrace();
		}
		return userInfoDto;
	}
	
	
	/**
	 * 基础支持中的access_token
	 *
	 * @return 返回值
	 */
	public synchronized static AccessToken getBaseAccessToken() {
		AccessToken accessToken = null;
		String urlStr = wechatConfig.getUrl().getAccessToken() + "appid=" + wechatConfig.getConfig().getAppid() + "&secret=" + wechatConfig.getConfig().getAppsecret();
		StringBuilder stringBuilder = httpsGet(urlStr);
		LOGGER.info("从腾讯服务器中获取的基础accessToken是【" + stringBuilder.toString() + "】");
		//将json字符串转成javaBean
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			accessToken = objectMapper.readValue(stringBuilder.toString(), AccessToken.class);
		} catch (IOException e) {
			LOGGER.error("json转对象发生异常" + e.getMessage());
		}
		return accessToken;
	}
	
	
	/**
	 * 从微信获取用户列表
	 * 每次请求最多获取10000条数据
	 *
	 * @param openid      第一个拉取的OPENID，不填默认从头开始拉取
	 * @param accessToken c
	 * @return f
	 */
	public static FindUserListDto findWechatUserList(String openid, String accessToken) {
		FindUserListDto findUserListDto = new FindUserListDto();
		if (StringUtils.hasText(accessToken)) {
			return findUserListDto;
		}
		String urlStr = wechatConfig.getUrl().getUserList() + "access_token=" + accessToken + "&next_openid=" + openid;
		StringBuilder stringBuilder = httpsGet(urlStr);
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			findUserListDto = objectMapper.readValue(stringBuilder.toString(), FindUserListDto.class);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return findUserListDto;
	}
	
	/**
	 * 获取所有的微信用户
	 *
	 * @param accessToken 参水
	 * @return 返回值
	 */
	public static List<String> findAllWechatUserList(String accessToken) {
		int searchTotal = 0;
		int total = 0;
		String nextOpenid = "";
		List<String> openidList = new ArrayList<>();
		FindUserListDto findUserListDto;
		
		findUserListDto = findWechatUserList(nextOpenid, accessToken);
		searchTotal += findUserListDto.getCount();
		total += findUserListDto.getTotal();
		openidList.addAll(findUserListDto.getData().getOpenid());
		nextOpenid = findUserListDto.getNext_openid();
		
		while (searchTotal < total) {
			findUserListDto = findWechatUserList(nextOpenid, accessToken);
			searchTotal += findUserListDto.getCount();
			total += findUserListDto.getTotal();
			openidList.addAll(findUserListDto.getData().getOpenid());
			nextOpenid = findUserListDto.getNext_openid();
		}
		return openidList;
	}
	
	/**
	 * 获取用户基本信息(UnionID机制)
	 *
	 * @param openid      参数
	 * @param accessToken 获取所有的微信用户
	 * @return 返回值
	 */
	public static UserInfoDto findUserInfoDto(String openid, String accessToken) {
		UserInfoDto userInfoDto = null;
		String url = wechatConfig.getUrl().getUserInfoUnionID() + accessToken + "&openid=" + openid;
		try {
			byte[] post = HttpsUtil.post(url, "", "UTF-8");
			ObjectMapper objectMapper = new ObjectMapper();
			userInfoDto = objectMapper.readValue(new String(post), UserInfoDto.class);
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}
		return userInfoDto;
	}
	
	/**
	 * 批量获取用户信息
	 *
	 * @param params      c
	 * @param accessToken c
	 * @return f
	 */
	public static UserInfoListDto findUserInfoList(String params, String accessToken) {
		UserInfoListDto userInfoListDto = null;
		String url = wechatConfig.getUrl().getUserInfoBatchget() + accessToken;
		try {
			byte[] post = HttpsUtil.post(url, params, "UTF-8");
			ObjectMapper objectMapper = new ObjectMapper();
			userInfoListDto = objectMapper.readValue(new String(post), UserInfoListDto.class);
			return userInfoListDto;
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}
		return null;
	}
	
	/**
	 * 生成二维码
	 *
	 * @param createQrcodeSendData 请求参数
	 * @param accessToken          基础的accessToken
	 * @return f
	 */
	public static CreateQrcodeDto createQrcode(CreateQrcodeSendData createQrcodeSendData, String accessToken) {
		
		String params = JackJsonUtils.toJson(createQrcodeSendData);
		
		CreateQrcodeDto createQrcodeDto = null;
		String url = wechatConfig.getUrl().getCreateQrcode() + accessToken;
		try {
			byte[] post = HttpsUtil.post(url, params, "UTF-8");
			ObjectMapper objectMapper = new ObjectMapper();
			createQrcodeDto = objectMapper.readValue(new String(post), CreateQrcodeDto.class);
			return createQrcodeDto;
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}
		return null;
	}
	
	
	/**
	 * 可以上传除了图文以外的永久素材
	 * 和临时素材
	 * 和图文消息内容里面的图片
	 *
	 * @param url  c
	 * @param file c
	 * @return f
	 */
	public static MaterialDto addMaterial(String url, MultipartFile file) {
		MaterialDto materialDto = null;
		byte[] bytes = new byte[0];
		try {
			bytes = HttpsUtil.postUploadPicToWeChat(url, file);
			ObjectMapper objectMapper = new ObjectMapper();
			materialDto = objectMapper.readValue(new String(bytes), MaterialDto.class);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return materialDto;
	}
	
	
	/**
	 * 上传图文消息
	 *
	 * @param params      c
	 * @param accessToken c
	 * @return f
	 */
	public static MaterialDto materialAddNews(String params, String accessToken) {
		MaterialDto materialDto = new MaterialDto();
		String url = wechatConfig.getUrl().getMaterialAddNews() + accessToken;
		try {
			byte[] post = HttpsUtil.post(url, params, "UTF-8");
			ObjectMapper objectMapper = new ObjectMapper();
			materialDto = objectMapper.readValue(new String(post), MaterialDto.class);
		} catch (Exception e) {
		}
		return materialDto;
	}
	
	
	/**
	 * 创建公众号菜单
	 * （将本地菜单同步到公众号）
	 *
	 * @param params      c
	 * @param accessToken c
	 * @return f
	 */
	public static Map createMenu(String params, String accessToken) {
		Map map = new HashMap();
		String url = wechatConfig.getUrl().getCreateMenu() + accessToken;
		try {
			byte[] post = HttpsUtil.post(url, params, "UTF-8");
			LOGGER.info("增加公众号菜单请求结果：【" + new String(post) + "】");
			ObjectMapper objectMapper = new ObjectMapper();
			map = objectMapper.readValue(new String(post), Map.class);
		} catch (Exception e) {
			map.put("errcode", 1);
			map.put("errmsg", "请求异常");
		}
		return map;
	}
	
	
	/**
	 * 创建tag
	 *
	 * @param tagName     c
	 * @param accessToken c
	 * @return f
	 */
	public static UserTagDto createTag(String tagName, String accessToken) {
		UserTagDto userTagDto = null;
		String params = "{\"tag\":{\"name\":\"" + tagName + "\"}} ";
		String url = wechatConfig.getUrl().getTagsCreate() + accessToken;
		try {
			byte[] post = HttpsUtil.post(url, params, "UTF-8");
			ObjectMapper objectMapper = new ObjectMapper();
			userTagDto = objectMapper.readValue(new String(post), UserTagDto.class);
			LOGGER.info("发送和客服消息的请求结果：【" + new String(post) + "】");
		} catch (Exception e) {
		}
		return userTagDto;
	}
	
	/**
	 * 更新tag
	 *
	 * @param wechatTagId c
	 * @param tagName     c
	 * @param accessToken c
	 * @return f
	 */
	public static UserTagDto updateTag(int wechatTagId, String tagName, String accessToken) {
		UserTagDto userTagDto = null;
		String params = "{\"tag\":{\"id\":" + wechatTagId + ",\"name\":\"" + tagName + "\"}}";
		String url = wechatConfig.getUrl().getTagsUpdate() + accessToken;
		try {
			byte[] post = HttpsUtil.post(url, params, "UTF-8");
			ObjectMapper objectMapper = new ObjectMapper();
			userTagDto = objectMapper.readValue(new String(post), UserTagDto.class);
			LOGGER.info("请求结果：【" + new String(post) + "】");
		} catch (Exception e) {
		}
		return userTagDto;
	}
	
	/**
	 * 批量为用户打标签
	 *
	 * @param batchTaggingSendData c
	 * @param accessToken          c
	 * @return f
	 */
	public static BaseDto batchtagging(BatchTaggingSendData batchTaggingSendData, String accessToken) {
		BaseDto baseDto = null;
		String params = JackJsonUtils.toJson(batchTaggingSendData);
		String url = wechatConfig.getUrl().getTagsBatchtagging() + accessToken;
		try {
			byte[] post = HttpsUtil.post(url, params, "UTF-8");
			ObjectMapper objectMapper = new ObjectMapper();
			baseDto = objectMapper.readValue(new String(post), BaseDto.class);
			LOGGER.info("批量为用户打标签：【" + new String(post) + "】");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return baseDto;
		
	}
	
	public static void sendMaterial(String accessToken) {
		String params =
				"{"
						+ "touser:oZp9Q1vyoNsl5jpQtsslvy6kXmRk,"
						+ "mpnews:{"
						+ "media_id:d_kuHr7C-LwS10llo1NjZpg3HGicqeU2Q6RWxBzeytA"
						+ "},"
						+ "msgtype:mpnews,"
						+ "send_ignore_reprint:0"
						+ "}";
		
		String url = "https://api.weixin.qq.com/cgi-bin/message/mass/preview?access_token=" + accessToken;
		try {
			byte[] post = HttpsUtil.post(url, params, "UTF-8");
			LOGGER.info("发送和客服消息的请求结果：【" + new String(post) + "】");
		} catch (Exception e) {
		}
		
		
	}

//
//	/**
//	 * 发送客服消息
//	 */
//	public static void sendKfMessage() {
//		KfMessageSendData kfMessageSendData = new KfMessageSendData();
//		kfMessageSendData.setMsgtype("news");
//		kfMessageSendData.setTouser("oZp9Q1vyoNsl5jpQtsslvy6kXmRk");
//		NewsBean newsBean = new NewsBean();
//		List<ArticlesBean> articlesBeans = new ArrayList<>();
//		ArticlesBean articlesBean = new ArticlesBean();
////        articlesBean.setTitle();
////        articlesBean.setDescription();
////        articlesBean.setUrl();
////        articlesBean.setPicurl();
//
//
//		newsBean.setArticles(articlesBeans);
//		kfMessageSendData.setNews(newsBean);
//		Map map = new HashMap();
//		String url = WeChatConfig.SEND_KF_MESSAGE + getBaseAccessToken().getAccess_token();
//		try {
//			byte[] post = HttpsUtil.post(url, StringUtils.paseObjToString(kfMessageSendData), "UTF-8");
//			Gson gson = new Gson();
//			map = gson.fromJson(new String(post), Map.class);
//			LOGGER.info("发送和客服消息的请求结果：【" + StringUtils.paseObjToString(map) + "】");
//		} catch (Exception e) {
//			map.put("errcode", 1);
//			map.put("errmsg", "请求异常");
//		}
////        return map;
//	}
//
//
	
	/**
	 * https协议的get请求
	 *
	 * @param urlStr c
	 * @return f
	 */
	public static StringBuilder httpsGet(String urlStr) {
		StringBuilder json = new StringBuilder();
		URL url = null;
		URLConnection uc = null;
		BufferedReader in = null;
		try {
			url = new URL(urlStr);
			uc = url.openConnection();
			in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				json.append(inputLine);
			}
		} catch (Exception e) {
			LOGGER.error("https协议的get请求发生异常");
			e.printStackTrace();
		} finally {
			try {
				in.close();
			} catch (Exception e) {
				LOGGER.error("关闭BufferedReader时发生异常");
				e.printStackTrace();
			}
		}
		return json;
	}
	
	
	/**
	 * 获取微信签名
	 * @param map 请求参数集合
	 * @return 微信请求签名串
	 */
//    public static String getSign(SortedMap<String,Object> packageParams){
//    	StringBuffer stringBuffer = new StringBuffer();
//        Set es = packageParams.entrySet();//字典序
//        Iterator it = es.iterator();
//        while (it.hasNext()) {
//            Map.Entry entry = (Map.Entry) it.next();
//            String k = (String) entry.getKey();
//            String v = (String) entry.getValue();
//            //为空不参与签名、参数名区分大小写
//            if (null != v && !"".equals(v) && !"sign".equals(k)  && !"key".equals(k)) {
//            	stringBuffer.append(k + "=" + v + "&");
//            }
//        }
//        //第二步拼接key，key设置路径：微信商户平台(pay.weixin.qq.com)-->账户设置-->API安全-->密钥设置
//        stringBuffer.append("key="+ WeChatUtil.APPSECRET_GZH);
//        String sign = Md5Util.md5(stringBuffer.toString()).toUpperCase();//MD5加密
//        System.out.println("--------------------------------------------------------------------------------");
//        System.out.println("sign = " + sign);
//        return sign;
//    }
	
	/**
	 * 获取IP地址
	 */
//    public static String getIpAddr(HttpServletRequest request) {
//        String ip = request.getHeader("x-forwarded-for");
//        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
//            ip = request.getHeader("Proxy-Client-IP");
//        }
//        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
//            ip = request.getHeader("WL-Proxy-Client-IP");
//        }
//        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
//            ip = request.getRemoteAddr();
//        }
//        return ip;
//    }
//	public static String getIpAddr(HttpServletRequest request) {
//		String ip = request.getHeader("X-Forwarded-For");
//		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
//			ip = request.getHeader("Proxy-Client-IP");
//		}
//		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
//			ip = request.getHeader("WL-Proxy-Client-IP");
//		}
//		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
//			ip = request.getHeader("HTTP_CLIENT_IP");
//		}
//		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
//			ip = request.getHeader("HTTP_X_FORWARDED_FOR");
//		}
//		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
//			ip = request.getRemoteAddr();
//		}
//
//		System.out.println("--------------------------------------------------------------------------");
//		System.out.println("addr_IP = " + ip);
//
//		String[] ips = ip.split(",");
//		return ips == null ? "0.0.0.1" : ips[0];
//	}
//
//	public static String getSign(SortedMap<String, Object> map) {
//		StringBuffer stringBuffer = new StringBuffer();
//		Set set = map.entrySet();//字典序
//		Iterator iterator = set.iterator();
//		while (iterator.hasNext()) {
//			Map.Entry entry = (Map.Entry) iterator.next();
//			String k = (String) entry.getKey();
//			Object v = entry.getValue();
//			// 参数中sign、key不参与签名加密
//			if (null != v && !"".equals(v) && !"sign".equals(k) && !"key".equals(k)) {
//				stringBuffer.append(k + "=" + v + "&");
//			}
//		}
//
//		System.out.println("--------------------------------------------------------------------------------");
//		System.out.println("1---stringBuffer = " + stringBuffer.toString());
//
//		stringBuffer.append("key=" + WeChatConfig.API_SECRET);
//		System.out.println("2---stringBuffer = " + stringBuffer.toString());
//
//
//		String sign = Md5Util.md5(stringBuffer.toString()).toUpperCase();
//
//		System.out.println("sign = " + sign);
//		return sign;
//	}
//
//
//	/**
//	 * 解析微信服务器发来的请求
//	 *
//	 * @param inputStream
//	 * @return 微信返回的参数集合
//	 */
//	public static SortedMap<String, Object> parseXml(InputStream inputStream) {
//		SortedMap<String, Object> map = new TreeMap();
//		try {
//			//获取request输入流
//			SAXReader reader = new SAXReader();
//			Document document = reader.read(inputStream);
//			//得到xml根元素
//			Element root = document.getRootElement();
//			//得到根元素所有节点
//			List<Element> elementList = root.elements();
//			//遍历所有子节点
//			for (Element element : elementList) {
//				map.put(element.getName(), element.getText());
//			}
//			//释放资源
//			inputStream.close();
//		} catch (Exception e) {
//			throw new ServiceException("微信工具类:解析xml异常", e);
//		}
//		return map;
//	}
//
//	/**
//	 * 扩展xstream,使其支持name带有"_"的节点
//	 */
//	public static XStream xStream = new XStream(new DomDriver("UTF-8", new XmlFriendlyNameCoder("-_", "_")));
//
//	/**
//	 * 请求参数转换成xml
//	 *
//	 * @param t
//	 * @return xml字符串
//	 */
//	public static <T> String sendDataToXml(T t) {
//		xStream.autodetectAnnotations(true);
//		xStream.alias("xml", WxPaySendData.class);
//		return xStream.toXML(t);
//	}
//
//	/**
//	 * 获取当前时间戳
//	 *
//	 * @return 当前时间戳字符串
//	 */
//	public static String getTimeStamp() {
//		return String.valueOf(System.currentTimeMillis() / 1000);
//	}
//
//	/**
//	 * 获取指定长度的随机字符串
//	 *
//	 * @param length
//	 * @return 随机字符串
//	 */
//	public static String getRandomStr(int length) {
//		String base = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
//		Random random = new Random();
//		StringBuffer sb = new StringBuffer();
//		for (int i = 0; i < length; i++) {
//			int number = random.nextInt(base.length());
//			sb.append(base.charAt(number));
//		}
//		return sb.toString();
//	}
}