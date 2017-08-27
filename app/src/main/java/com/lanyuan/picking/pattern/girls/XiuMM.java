package com.lanyuan.picking.pattern.girls;

import android.graphics.Color;

import com.lanyuan.picking.common.bean.PicInfo;
import com.lanyuan.picking.pattern.MultiPicturePattern;
import com.lanyuan.picking.ui.contents.ContentsActivity;
import com.lanyuan.picking.ui.detail.DetailActivity;
import com.lanyuan.picking.common.bean.AlbumInfo;
import com.lanyuan.picking.ui.menu.Menu;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class XiuMM implements MultiPicturePattern {
    @Override
    public String getWebsiteName() {
        return "秀美眉";
    }

    @Override
    public String getCategoryCoverUrl() {
        return "http://www.xiumm.org/themes/sense/images/logo.png";
    }

    @Override
    public int getBackgroundColor() {
        return Color.BLACK;
    }

    @Override
    public String getBaseUrl(List<Menu> menuList, int position) {
        return "http://www.xiumm.org/";
    }

    @Override
    public List<Menu> getMenuList() {
        List<Menu> menuList = new ArrayList<>();
        menuList.add(new Menu("首页", "http://www.xiumm.org/"));
        menuList.add(new Menu("尤果", "http://www.xiumm.org/albums/UGirls.html"));
        menuList.add(new Menu("菠萝社", "http://www.xiumm.org/albums/BoLoli.html"));
        menuList.add(new Menu("萌缚", "http://www.xiumm.org/albums/MF.html"));
        menuList.add(new Menu("魅妍社", "http://www.xiumm.org/albums/MiStar.html"));
        menuList.add(new Menu("爱蜜社", "http://www.xiumm.org/albums/imiss.html"));
        menuList.add(new Menu("嗲囡囡", "http://www.xiumm.org/albums/FeiLin.html"));
        menuList.add(new Menu("优星馆", "http://www.xiumm.org/albums/UXING.html"));
        menuList.add(new Menu("模范学院", "http://www.xiumm.org/albums/MFStar.html"));
        menuList.add(new Menu("美媛馆", "http://www.xiumm.org/albums/MyGirl.html"));
        menuList.add(new Menu("秀人网", "http://www.xiumm.org/albums/XiuRen.html"));
        menuList.add(new Menu("V女郎", "http://www.xiumm.org/albums/vgirl.html"));
        menuList.add(new Menu("青豆客", "http://www.xiumm.org/albums/TGod.html"));
        menuList.add(new Menu("顽味生活", "http://www.xiumm.org/albums/Taste.html"));
        menuList.add(new Menu("DK御女郎", "http://www.xiumm.org/albums/DKGirl.html"));
        menuList.add(new Menu("薄荷叶", "http://www.xiumm.org/albums/MintYe.html"));
        menuList.add(new Menu("糖果画报", "http://www.xiumm.org/albums/CANDY.html"));
        menuList.add(new Menu("糖果画报", "http://www.xiumm.org/albums/CANDY.html"));
        menuList.add(new Menu("尤蜜荟", "http://www.xiumm.org/albums/YOUMI.html"));
        menuList.add(new Menu("猫萌榜", "http://www.xiumm.org/albums/MICAT.html"));
        return menuList;
    }

    @Override
    public Map<ContentsActivity.parameter, Object> getContent(String baseUrl, String currentUrl, byte[] result, Map<ContentsActivity.parameter, Object> resultMap) throws UnsupportedEncodingException {
        List<AlbumInfo> urls = new ArrayList<>();
        Document document = Jsoup.parse(new String(result, "utf-8"));
        Elements elements = document.select("div.album");
        for (Element element : elements) {
            AlbumInfo temp = new AlbumInfo();

            Elements title = element.select("span.name");
            if (title.size() > 0)
                temp.setTitle(title.get(0).text());

            Elements album = element.select(".pic_box a");
            temp.setAlbumUrl(album.attr("href"));
            Elements pic = album.select("img");
            if (pic.size() > 0)
                temp.setPicUrl(pic.get(0).attr("src"));
            urls.add(temp);
        }
        resultMap.put(ContentsActivity.parameter.CURRENT_URL, currentUrl);
        resultMap.put(ContentsActivity.parameter.RESULT, urls);
        return resultMap;
    }

    @Override
    public String getContentNext(String baseUrl, String currentUrl, byte[] result) throws UnsupportedEncodingException {
        Document document = Jsoup.parse(new String(result, "utf-8"));
        Elements elements = document.select(".paginator span.next a");
        if (elements.size() > 0)
            return baseUrl + elements.get(0).attr("href");
        return "";
    }

    @Override
    public Map<DetailActivity.parameter, Object> getDetailContent(String baseUrl, String currentUrl, byte[] result, Map<DetailActivity.parameter, Object> resultMap) throws UnsupportedEncodingException {
        List<PicInfo> urls = new ArrayList<>();
        Document document = Jsoup.parse(new String(result, "utf-8"));

        Elements title = document.select("div.album_desc div.inline");
        String sTitle = "";
        if (title.size() > 0)
            sTitle = title.get(0).text();

        Elements elements = document.select(".gallary_item .pic_box img");
        for (Element element : elements) {
            urls.add(new PicInfo(baseUrl + element.attr("src")).setTitle(sTitle));
        }
        resultMap.put(DetailActivity.parameter.CURRENT_URL, currentUrl);
        resultMap.put(DetailActivity.parameter.RESULT, urls);
        return resultMap;
    }

    @Override
    public String getDetailNext(String baseUrl, String currentUrl, byte[] result) throws UnsupportedEncodingException {
        Document document = Jsoup.parse(new String(result, "utf-8"));
        Elements elements = document.select(".paginator span.next a");
        if (elements.size() > 0)
            return baseUrl + elements.get(0).attr("href");
        return "";
    }
}
