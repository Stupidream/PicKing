package com.lanyuan.picking.config;

import android.os.Environment;

import com.lanyuan.picking.pattern.BasePattern;
import com.lanyuan.picking.pattern.anime.AKabe;
import com.lanyuan.picking.pattern.anime.Acg12;
import com.lanyuan.picking.pattern.anime.AnimePic;
import com.lanyuan.picking.pattern.anime.AoJiao;
import com.lanyuan.picking.pattern.anime.Apic;
import com.lanyuan.picking.pattern.anime.KonaChan;
import com.lanyuan.picking.pattern.anime.MiniTokyo;
import com.lanyuan.picking.pattern.anime.Yande;
import com.lanyuan.picking.pattern.anime.ZeroChan;
import com.lanyuan.picking.pattern.boys.Nanrentu;
import com.lanyuan.picking.pattern.girls.DuowanCos;
import com.lanyuan.picking.pattern.girls.JDlingyu;
import com.lanyuan.picking.pattern.girls.MM131;
import com.lanyuan.picking.pattern.girls.RosiMM;
import com.lanyuan.picking.pattern.girls.XiuMM;
import com.lanyuan.picking.pattern.girls.Yesky;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppConfig {

    public static final String DOWNLOAD_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pictures/Papaya/";

    public static final String DEFAULT_COLOR = "default";
    public static final String BLUE_COLOR = "blue";
    public static final String ORANGE_COLOR = "orange";
    public static final String BROWN_COLOR = "brown";
    public static final String PURPLE_COLOR = "purple";
    public static final String CYAN_COLOR = "cyan";
    public static final String GREEN_COLOR = "green";
    public static final String YELLOW_COLOR = "yellow";
    public static final String GREY_COLOR = "grey";

    public static final String show_tips = "showTips";
    public static final String choose_theme = "chooseTheme";
    public static final String load_pic_swipe = "loadPicSwipe";
    public static final String click_to_back = "clickToBack";
    public static final String download_path = "downloadPath";
    public static final String cache_size = "cacheSize";
    public static final String share_model = "shareModel";
    public static final String hide_pic = "hidePic";

    public static final String anime_patterns = "animePatterns";
    public static final String boys_patterns = "boysPatterns";
    public static final String girls_patterns = "girlsPatterns";

    public static Map categoryList;

    static {
        categoryList = new HashMap();
        List<BasePattern> animePatterns = new ArrayList<BasePattern>() {{
            add(new KonaChan());
            add(new Apic());
            add(new Acg12());
            add(new AoJiao());
            add(new ZeroChan());
            // add(new Yande());
            add(new AKabe());
            add(new AnimePic());
            add(new MiniTokyo());
        }};
        List<BasePattern> girlsPatterns = new ArrayList<BasePattern>() {{
            add(new JDlingyu());
            add(new MM131());
            add(new XiuMM());
            add(new RosiMM());
            add(new Yesky());
            add(new DuowanCos());
        }};
        List<BasePattern> boysPatterns = new ArrayList<BasePattern>() {{
            add(new Nanrentu());
        }};
        categoryList.put(anime_patterns, animePatterns);
        categoryList.put(girls_patterns, girlsPatterns);
        categoryList.put(boys_patterns, boysPatterns);
    }

}
