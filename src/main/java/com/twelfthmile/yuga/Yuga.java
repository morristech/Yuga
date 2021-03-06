package com.twelfthmile.yuga;

import com.twelfthmile.yuga.types.*;
import com.twelfthmile.yuga.utils.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by johnjoseph on 19/03/17.
 */

public class Yuga {

    public static RootTrie root;
    static boolean D_DEBUG = false;

    public static void init() {
        if (root == null) {
            root = new RootTrie();
            root.next.put("FSA_MONTHS", new GenTrie());
            root.next.put("FSA_DAYS", new GenTrie());
            root.next.put("FSA_TIMEPRFX", new GenTrie());
            root.next.put("FSA_AMT", new GenTrie());
            root.next.put("FSA_TIMES", new GenTrie());
            root.next.put("FSA_TZ", new GenTrie());
            root.next.put("FSA_DAYSFFX", new GenTrie());
            root.next.put("FSA_UPI", new GenTrie());
            seeding(Constants.FSA_MONTHS, root.next.get("FSA_MONTHS"));
            seeding(Constants.FSA_DAYS, root.next.get("FSA_DAYS"));
            seeding(Constants.FSA_TIMEPRFX, root.next.get("FSA_TIMEPRFX"));
            seeding(Constants.FSA_AMT, root.next.get("FSA_AMT"));
            seeding(Constants.FSA_TIMES, root.next.get("FSA_TIMES"));
            seeding(Constants.FSA_TZ, root.next.get("FSA_TZ"));
            seeding(Constants.FSA_DAYSFFX, root.next.get("FSA_DAYSFFX"));
            seeding(Constants.FSA_UPI, root.next.get("FSA_UPI"));
        }
    }

    private static void seeding(String type, GenTrie root) {
        GenTrie t;
        int c = 0;
        for (String fsaCldr : type.split(",")) {
            c++;
            t = root;
            int len = fsaCldr.length();
            for (int i = 0; i < len; i++) {
                char ch = fsaCldr.charAt(i);
                t.child = true;
                if (!t.next.containsKey(ch))
                    t.next.put(ch, new GenTrie());
                t = t.next.get(ch);
                if (i == len - 1) {
                    t.leaf = true;
                    t.token = fsaCldr.replace(";", "");
                } else if (i < (len - 1) && fsaCldr.charAt(i + 1) == 59) { //semicolon
                    t.leaf = true;
                    t.token = fsaCldr.replace(";", "");
                    i++;//to skip semicolon
                }
            }
        }
    }

    /**
     * Returns Pair of index upto which date was read and the date object
     * @param str date string
     * @return A-> last index for date string, b-> date object
     * returns null if string is not of valid date format
     */
    public static Pair<Integer, Date> parseDate(String str) throws ParseException {
        HashMap<String, String> configMap = generateDefaultConfig();
        Pair<Integer, FsaContextMap> p = parseInternal(str, configMap);
        if (p == null)
            throw new ParseException("[com.twelfthmile.yuga.Yuga] Wrong date format: " + str);
        Date d = p.getB().getDate(configMap);
        if (d == null)
            throw new ParseException("[com.twelfthmile.yuga.Yuga] Wrong date format: " + str);
        return new Pair<>(p.getA(), d);
    }

    /**
     * Returns Pair of index upto which date was read and the date object
     *
     * @param str    date string
     * @param config pass the message date string for defaulting
     * @return A-> last index for date string, b-> date object
     * returns null if string is not of valid date format
     */
    public static Pair<Integer, Date> parseDate(String str, HashMap<String, String> config) throws ParseException {
        Pair<Integer, FsaContextMap> p = parseInternal(str, config);
        if (p == null)
            throw new ParseException("[com.twelfthmile.yuga.Yuga] Wrong date format: " + str);
        Date d = p.getB().getDate(config);
        if (d == null)
            throw new ParseException("[com.twelfthmile.yuga.Yuga] Wrong date format: " + str);
        return new Pair<>(p.getA(), d);
    }

    /**
     * Returns Response containing data-type, captured string and index upto which data was read
     *
     * @param str    string to be parsed
     * @param config config for parsing (Eg: date-defaulting)
     * @return com.twelfthmile.yuga.Yuga Response type
     */
    public static Response parse(String str, HashMap<String, String> config) throws ParseException {
        Pair<Integer, FsaContextMap> p = parseInternal(str, config);
        if (p == null)
            throw new ParseException("[com.twelfthmile.yuga.Yuga] Unsupported format: " + str);
        Pair<String, Object> pr = prepareResult(str, p, config);
        return new Response(pr.getA(), p.getB().getValMap(), pr.getB(), p.getA());
    }

    /**
     * Returns Response containing data-type, captured string and index upto which data was read
     *
     * @param str string to be parsed
     * @return com.twelfthmile.yuga.Yuga Response type
     */
    public static Response parse(String str) throws ParseException {
        HashMap<String, String> configMap = generateDefaultConfig();
        Pair<Integer, FsaContextMap> p = parseInternal(str, configMap);
        if (p == null)
            throw new ParseException("[com.twelfthmile.yuga.Yuga] Unsupported format: " + str);
        Pair<String, Object> pr = prepareResult(str, p, configMap);
        return new Response(pr.getA(), p.getB().getValMap(), pr.getB(), p.getA());
    }

    // Pair <Type,String>
    private static Pair<String, Object> prepareResult(String str, Pair<Integer, FsaContextMap> p, HashMap<String, String> config) {
        int index = p.getA();
        FsaContextMap map = p.getB();
        if (map.getType().equals(Constants.TY_DTE)) {
            if (map.contains(Constants.DT_MMM) && map.size() < 3)//may fix
                return new Pair<String, Object>(Constants.TY_STR, str.substring(0, index));
            if (map.contains(Constants.DT_HH) && map.contains(Constants.DT_mm) && !map.contains(Constants.DT_D) && !map.contains(Constants.DT_DD) && !map.contains(Constants.DT_MM) && !map.contains(Constants.DT_MMM) && !map.contains(Constants.DT_YY) && !map.contains(Constants.DT_YYYY)) {
                map.setType(Constants.TY_TME, null);
                map.setVal("time", map.get(Constants.DT_HH) + ":" + map.get(Constants.DT_mm));
                return new Pair<String, Object>(Constants.TY_TME, str.substring(0, index));
            }
            Date d = map.getDate(config);
            if (d != null)
                return new Pair<String, Object>(p.getB().getType(), d);
            else
                return new Pair<String, Object>(Constants.TY_STR, str.substring(0, index));
        } else {
            if (map.get(map.getType()) != null) {
                if (map.getType().equals(Constants.TY_ACC) && config.containsKey(Constants.YUGA_SOURCE_CONTEXT) && config.get(Constants.YUGA_SOURCE_CONTEXT).equals(Constants.YUGA_SC_CURR))
                    return new Pair<String, Object>(Constants.TY_AMT, map.get(map.getType()).replaceAll("X", ""));
                return new Pair<String, Object>(p.getB().getType(), map.get(map.getType()));
            }
            else
                return new Pair<String, Object>(p.getB().getType(), str.substring(0, index));

        }
    }

    private static HashMap<String, String> generateDefaultConfig() {
        HashMap<String, String> config = new HashMap<String, String>();
        config.put(Constants.YUGA_CONF_DATE, Constants.dateTimeFormatter().format(new Date()));
        return config;
    }

    public static void main(String[] s) {
        try {
//            parse("29Nov17").print();
//            parse("08 May").print();
//            parse("August 15").print();
//            parse("Sep 2017").print();
//            parse("09:40 PM May 21, 2017").print();
//            parse("18:10 (Thur, 12 Oct)").print();
//            parse("07:10 on Wed 14 Jun 17").print();
//            parse("Wed Dec 20 17:26:25 GMT+05:30 2017").print();
//            L.msg(parse("2017-09-03:02:15:44").print());
//            parse("Mon Sep 04 13:47:13 IST 2017").print();
//            parse("07 Jan 18, 06:05").print();
//            L.msg(parse("9/21/2017").print());
//            L.msg(parse("21 2017").print());
//            L.msg(parse("Friday, 22nd September 4:30 PM").print());
//            L.msg(parse("0.017%").print());
//            parse("18/09/2017").print();
//            parse("12/05").print();
//            parse("12/05/16").print();
//            parse("2016/05/12").print();
//            parse("12Aug2017").print();
//            L.msg(parse("04-Jul-17").print());
//            L.msg(parse("16-Feb-2017 15:26:58").print());
//            parse("07 Sep 2017").print();
//            L.msg(parse("21-Aug-17 19:21 Hrs").print());
//            parse("27Jan").print();
//            parse("Sun, 3 Sep, 2017 02:00pm").print();
//            L.msg(parse("23:35 hrs").print());
//            parse("24-10-2017 15:05:57").print();
//            parse("2017-02-18:14:26:00").print();
//            L.msg(parse("22-Oct-2017 21:10").print());
//            L.msg(parse("15:59:24").print());
//            L.msg(parse("30-Nov 14:54").print());
//            parse("06/JAN/17").print();
//            parse("Dec 18th").print();
//            parse("100abc").print();
//            parse("+917032641284").print();
//            parse("7032641284").print();
//            parse("1").print();
//            parse("12").print();
//            parse("123").print();
//            parse("123.00").print();
//            parse("1,23,000.00").print();
//            parse("2000").print();
//            parse("2000.00").print();
//            parse("100%").print();
//            parse("1,00,00,000.00").print();
//            parse("1.23").print();
//            parse("**2657").print();
//            parse("123**2657").print();
//            L.msg(parse("-351.00").print());
//            parse("-351").print();
//            L.msg(parse("1-800-2703311").print());
//            L.msg(parse("1800-2703311").print());
//            parse("18002703311").print();
//            L.msg(parse("91-7032641284").print());
//            L.msg(parse("0484-2340606").print());
//            L.msg(parse("040 30425500").print());
//            L.msg(parse("563C35").print());
//            parse("(080) 39412345").print();//unsupported
//            parse("080 26703300").print();
//            parse("26700156").print();
//            parse("35K").print();
//            parse("1Lac").print();
//            L.msg(parse("10hrs").print());
//            L.msg(parse("2045 HRS").print());
//            L.msg(parse("0316-03624669-190001").print());
//            L.msg(parse("461721XXXXXX7514").print());
//            L.msg(parse("4617-21XX-XXXX-7514").print());
//            L.msg(parse("0820/0950").print());
//            L.msg(parse("0820-0950").print());
//            L.msg(parse("24 hrs").print());
//            L.msg(parse("Wed, 29 Mar, 2017 12:30 pm").print());
//            L.msg(parse("1800 180 3333").print());
//            L.msg(parse("18602660333").print());
//            L.msg(parse("95553 95553").print());
//            L.msg(parse("Thu 18, May 2017").print());
//            L.msg(parse("Thursday, May 18, 2017").print());
//            L.msg(parse("15th Dec").print());
//            L.msg(parse("1812+GST").print());
//            L.msg(parse("Mon, 12 Mar 2018 16:56:45 GMT").print());
//            L.msg(parse("2.10.2017").print());
//            L.msg(parse("7-2209").print());
//            L.msg(parse("12.41KM").print());
//            L.msg(parse("1.16KM").print());
//            L.msg(parse("32 minutes").print());
            L.msg(parse("16 -Nov -17").print());
        } catch (ParseException e) {
            L.msg(e.getMessage());
        }
    }

    private static Pair<Integer, FsaContextMap> parseInternal(String str, HashMap<String, String> config) {
        init();
        int state = 1, i = 0;
        Pair<Integer, String> p;
        char c;
        FsaContextMap map = new FsaContextMap();
        DelimeterStack delimeterStack = new DelimeterStack();
        str = str.toLowerCase();
        int counter = 0;
        while (state > 0 && i < str.length()) {
            c = str.charAt(i);
            switch (state) {
                case 1:
                    if (Util.isNumber(c)) {
                        map.setType(Constants.TY_NUM, null);
                        map.put(Constants.TY_NUM, c);
                        state = 2;
                    } else if ((p = Util.checkTypes("FSA_MONTHS", str.substring(i))) != null) {
                        map.setType(Constants.TY_DTE, null);
                        map.put(Constants.DT_MMM, p.getB());
                        i += p.getA();
                        state = 33;
                    } else if ((p = Util.checkTypes("FSA_DAYS", str.substring(i))) != null) {
                        map.setType(Constants.TY_DTE, null);
                        map.put(Constants.DT_DD, p.getB());
                        i += p.getA();
                        state = 30;
                    } else if (c == Constants.CH_HYPH) {//it could be a negative number
                        state = 37;
                    } else {
                        state = accAmtNumPct(str, i, map, config);
                        if (map.getType() == null)
                            return null;
                        if (state == -1 && !map.getType().equals(Constants.TY_PCT))
                            i = i - 1;
                    }
                    break;
                case 2:
                    if (Util.isNumber(c)) {
                        map.append(c);
                        state = 3;
                    } else if (Util.isTimeOperator(c)) {
                        delimeterStack.push(c);
                        map.setType(Constants.TY_DTE, Constants.DT_HH);
                        state = 4;
                    } else if (Util.isDateOperator(c) || c == Constants.CH_COMA) {
                        delimeterStack.push(c);
                        map.setType(Constants.TY_DTE, Constants.DT_D);
                        state = 16;
                    } else if ((p = Util.checkTypes("FSA_MONTHS", str.substring(i))) != null) {
                        map.setType(Constants.TY_DTE, Constants.DT_D);
                        map.put(Constants.DT_MMM, p.getB());
                        i += p.getA();
                        state = 24;
                    } else {
                        state = accAmtNumPct(str, i, map, config);
                        if (state == -1 && !map.getType().equals(Constants.TY_PCT))
                            i = i - 1;
                    }
                    break;
                case 3:
                    if (Util.isNumber(c)) {
                        map.append(c);
                        state = 8;
                    } else if (Util.isTimeOperator(c)) {
                        delimeterStack.push(c);
                        map.setType(Constants.TY_DTE, Constants.DT_HH);
                        state = 4;
                    } else if (Util.isDateOperator(c) || c == Constants.CH_COMA) {
                        delimeterStack.push(c);
                        map.setType(Constants.TY_DTE, Constants.DT_D);
                        state = 16;
                    } else if ((p = Util.checkTypes("FSA_MONTHS", str.substring(i))) != null) {
                        map.setType(Constants.TY_DTE, Constants.DT_D);
                        map.put(Constants.DT_MMM, p.getB());
                        i += p.getA();
                        state = 24;
                    } else if ((p = Util.checkTypes("FSA_DAYSFFX", str.substring(i))) != null) {
                        map.setType(Constants.TY_DTE, Constants.DT_D);
                        i += p.getA();
                        state = 32;
                    } else {
                        state = accAmtNumPct(str, i, map, config);
                        if (state == -1 && !map.getType().equals(Constants.TY_PCT))
                            i = i - 1;
                    }
                    break;
                case 4: //hours to mins
                    if (Util.isNumber(c)) {
                        map.upgrade(c);//hh to mm
                        state = 5;
                    } else { //saw a colon randomly, switch back to num from hours
                        if (!map.contains(Constants.DT_MMM))
                            map.setType(Constants.TY_NUM, Constants.TY_NUM);
                        i = i - 2; //move back so that colon is omitted
                        state = -1;
                    }
                    break;
                case 5:
                    if (Util.isNumber(c)) {
                        map.append(c);
                        state = 5;
                    } else if (c == Constants.CH_COLN)
                        state = 6;
                    else if (c == 'a' && (i + 1) < str.length() && str.charAt(i + 1) == 'm') {
                        i = i + 1;
                        state = -1;
                    } else if (c == 'p' && (i + 1) < str.length() && str.charAt(i + 1) == 'm') {
                        map.put(Constants.DT_HH, String.valueOf(Integer.parseInt(map.get(Constants.DT_HH)) + 12));
                        i = i + 1;
                        state = -1;
                    } else if ((p = Util.checkTypes("FSA_TIMES", str.substring(i))) != null) {
                        i += p.getA();
                        state = -1;
                    } else
                        state = 7;
                    break;
                case 6: //for seconds
                    if (Util.isNumber(c)) {
                        map.upgrade(c);
                        if ((i + 1) < str.length() && Util.isNumber(str.charAt(i + 1)))
                            map.append(str.charAt(i + 1));
                        i = i + 1;
                        state = -1;
                    } else
                        state = -1;
                    break;
                case 7:
                    if (c == 'a' && (i + 1) < str.length() && str.charAt(i + 1) == 'm') {
                        i = i + 1;
                        int hh = Integer.parseInt(map.get(Constants.DT_HH));
                        if (hh == 12)
                            map.put(Constants.DT_HH, String.valueOf(0));
                    }
                    else if (c == 'p' && (i + 1) < str.length() && str.charAt(i + 1) == 'm') {
                        int hh = Integer.parseInt(map.get(Constants.DT_HH));
                        if (hh != 12)
                            map.put(Constants.DT_HH, String.valueOf(hh + 12));
                        i = i + 1;
                    } else if ((p = Util.checkTypes("FSA_TIMES", str.substring(i))) != null) {
                        i += p.getA();
                    } else
                        i = i - 2;
                    state = -1;
                    break;
                case 8:
                    if (Util.isNumber(c)) {
                        map.append(c);
                        state = 9;
                    } else {
                        state = accAmtNumPct(str, i, map, config);
                        if ((c == Constants.CH_SPACE || c == Constants.CH_HYPH) && state == -1 && (i + 1) < str.length() && Util.isNumber(str.charAt(i + 1)))
                            state = 12;
                        else
                        if (state == -1 && !map.getType().equals(Constants.TY_PCT))
                            i = i - 1;
                    }
                    break;
                case 9:
                    if (Util.isDateOperator(c)) {
                        delimeterStack.push(c);
                        state = 25;
                    }
                    //handle for num case
                    else if (Util.isNumber(c)) {
                        map.append(c);
                        counter = 5;
                        state = 15;
                    } else {
                        state = accAmtNumPct(str, i, map, config);
                        if (state == -1 && !map.getType().equals(Constants.TY_PCT)) {//NUM
                            i = i - 1;
                        }
                    }
                    break;
                case 10:
                    if (Util.isNumber(c)) {
                        map.append(c);
                        map.setType(Constants.TY_AMT, Constants.TY_AMT);
                        state = 14;
                    } else { //saw a fullstop randomly
                        map.pop();//remove the dot which was appended
                        i = i - 2;
                        state = -1;
                    }
                    break;
                case 11:
                    if (c == 42 || c == 88 || c == 120)//*Xx
                        map.append('X');
                    else if (c == Constants.CH_HYPH)
                        state = 11;
                    else if (Util.isNumber(c)) {
                        map.append(c);
                        state = 13;
                    } else if (c == ' ' && ((i + 1) < str.length() && (str.charAt(i + 1) == 42 || str.charAt(i + 1) == 88 || str.charAt(i + 1) == 120 || Util.isNumber(str.charAt(i+1))) ))
                        state = 11;
                    else {
                        i = i - 1;
                        state = -1;
                    }
                    break;
                case 12:
                    if (Util.isNumber(c)) {
                        map.setType(Constants.TY_AMT, Constants.TY_AMT);
                        map.append(c);
                    } else if (c == Constants.CH_COMA) //comma
                        state = 12;
                    else if (c == Constants.CH_FSTP) { //dot
                        map.append(c);
                        state = 10;
                    } else if (c == Constants.CH_HYPH && (i + 1) < str.length() && Util.isNumber(str.charAt(i + 1))) {
                        state = 39;
                    } else {
                        if (i - 1 > 0 && str.charAt(i - 1) == Constants.CH_COMA)
                            i = i - 2;
                        else
                            i = i - 1;
                        state = -1;
                    }
                    break;
                case 13:
                    if (Util.isNumber(c))
                        map.append(c);
                    else if (c == 42 || c == 88 || c == 120)//*Xx
                        map.append('X');
                    else if (c == Constants.CH_FSTP && config.containsKey(Constants.YUGA_SOURCE_CONTEXT) && config.get(Constants.YUGA_SOURCE_CONTEXT).equals(Constants.YUGA_SC_CURR)) { //LIC **150.00 fix
                        map.setType(Constants.TY_AMT, Constants.TY_AMT);
                        map.put(Constants.TY_AMT, map.get(Constants.TY_AMT).replaceAll("X", ""));
                        map.append(c);
                        state = 10;
                    } else {
                        i = i - 1;
                        state = -1;
                    }
                    break;
                case 14:
                    if (Util.isNumber(c)) {
                        map.append(c);
                    } else if (c == Constants.CH_PCT) {
                        map.setType(Constants.TY_PCT, Constants.TY_PCT);
                        state = -1;
                    } else if (c == 'k' && (i + 1) < str.length() && str.charAt(i + 1) == 'm') {
                        map.setType(Constants.TY_DST, Constants.TY_DST);
                        i += 1;
                        state = -1;
                    } else {
                        if (c == Constants.CH_FSTP && ((i + 1) < str.length() && Util.isNumber(str.charAt(i + 1)))) {
                            String samt = map.get(map.getType());
                            if (samt.contains(".")) {
                                String[] samtarr = samt.split("\\.");
                                if (samtarr.length == 2) {
                                    map.setType(Constants.TY_DTE);
                                    map.put(Constants.DT_D, samtarr[0]);
                                    map.put(Constants.DT_MM, samtarr[1]);
                                    state = 19;
                                    break;
                                }
                            }
                        }
                        i = i - 1;
                        state = -1;
                    }
                    break;
                case 15:
                    if (Util.isNumber(c)) {
                        counter++;
                        map.append(c);
                    } else if (c == Constants.CH_COMA) //comma
                        state = 12;
                    else if (c == Constants.CH_FSTP) { //dot
                        map.append(c);
                        state = 10;
                    } else if ((c == 42 || c == 88 || c == 120) && (i + 1) < str.length() && ((Util.isNumber(str.charAt(i + 1)) || str.charAt(i + 1) == Constants.CH_HYPH) || str.charAt(i + 1) == 42 || str.charAt(i + 1) == 88 || str.charAt(i + 1) == 120)) {//*Xx
                        map.setType(Constants.TY_ACC, Constants.TY_ACC);
                        map.append('X');
                        state = 11;
                    } else if (c == Constants.CH_SPACE && (i + 2) < str.length() && Util.isNumber(str.charAt(i + 1)) && Util.isNumber(str.charAt(i + 2))) {
                        state = 41;
                    } else {
                        i = i - 1;
                        state = -1;
                    }
                    break;
                case 16:
                    if (Util.isNumber(c)) {
                        map.upgrade(c);
                        state = 17;
                    } else if (c == Constants.CH_SPACE || c == Constants.CH_COMA)
                        state = 16;
                    else if ((p = Util.checkTypes("FSA_MONTHS", str.substring(i))) != null) {
                        map.put(Constants.DT_MMM, p.getB());
                        i += p.getA();
                        state = 24;
                    }
                    //we should handle amt case, where comma led to 16 as opposed to 12
                    else if (c == Constants.CH_FSTP) { //dot
                        map.setType(Constants.TY_NUM, Constants.TY_NUM);
                        map.append(c);
                        state = 10;
                    } else if (i > 0 && (p = Util.checkTypes("FSA_TIMES", str.substring(i))) != null) {
                        map.setType(Constants.TY_TME, null);
                        String s = str.substring(0, i);
                        if (p.getB().equals("mins") || p.getB().equals("minutes"))
                            s = "00" + s;
                        extractTime(s, map.getValMap());
                        i = i + p.getA();
                        state = -1;
                    } else {//this is just a number, not a date
                        //to cater to 16 -Nov -17
                        if (delimeterStack.pop() == Constants.CH_SPACE && c == Constants.CH_HYPH && i + 1 < str.length() && (Util.isNumber(str.charAt(i + 1)) || (p = Util.checkTypes("FSA_MONTHS", str.substring(i + 1))) != null)) {
                            state = 16;
                        } else {
                            map.setType(Constants.TY_NUM, Constants.TY_NUM);
                            int j = i;
                            while (!Util.isNumber(str.charAt(j)))
                                j--;
                            i = j;
                            state = -1;
                        }
                    }
                    break;
                case 17:
                    if (Util.isNumber(c)) {
                        map.append(c);
                        state = 18;
                    } else if (Util.isDateOperator(c)) {
                        delimeterStack.push(c);
                        state = 19;
                    }
                    //we should handle amt case, where comma led to 16,17 as opposed to 12
                    else if (c == Constants.CH_COMA && delimeterStack.pop() == Constants.CH_COMA) { //comma
                        map.setType(Constants.TY_NUM, Constants.TY_NUM);
                        state = 12;
                    } else if (c == Constants.CH_FSTP && delimeterStack.pop() == Constants.CH_COMA) { //dot
                        map.setType(Constants.TY_NUM, Constants.TY_NUM);
                        map.append(c);
                        state = 10;
                    } else {
                        map.setType(Constants.TY_STR, Constants.TY_STR);
                        i = i - 1;
                        state = -1;
                    }
                    break;
                case 18:
                    if (Util.isDateOperator(c)) {
                        delimeterStack.push(c);
                        state = 19;
                    }
                    //we should handle amt case, where comma led to 16,17 as opposed to 12
                    else if (Util.isNumber(c) && delimeterStack.pop() == Constants.CH_COMA) {
                        map.setType(Constants.TY_NUM, Constants.TY_NUM);
                        state = 12;
                        map.append(c);
                    } else if (Util.isNumber(c) && delimeterStack.pop() == Constants.CH_HYPH) {
                        map.setType(Constants.TY_NUM, Constants.TY_NUM);
                        state = 42;
                        map.append(c);
                    } else if (c == Constants.CH_COMA && delimeterStack.pop() == Constants.CH_COMA) { //comma
                        map.setType(Constants.TY_NUM, Constants.TY_NUM);
                        state = 12;
                    } else if (c == Constants.CH_FSTP && delimeterStack.pop() == Constants.CH_COMA) { //dot
                        map.setType(Constants.TY_NUM, Constants.TY_NUM);
                        map.append(c);
                        state = 10;
                    } else {
                        map.setType(Constants.TY_STR, Constants.TY_STR);
                        i = i - 1;
                        state = -1;
                    }
                    break;
                case 19: //year
                    if(Util.isNumber(c)) {
                        map.upgrade(c);
                        state = 20;
                    } else {
                        i = i - 2;
                        state = -1;
                    }
                    break;
                case 20: //year++
                    if (Util.isNumber(c)) {
                        map.append(c);
                        state = 21;
                    } else if (c == ':') {
                        map.convert(Constants.DT_YY, Constants.DT_HH);
                        state = 4;
                    } else {
                        map.remove(Constants.DT_YY);//since there is no one number year
                        i = i - 1;
                        state = -1;
                    }
                    break;
                case 21:
                    if (Util.isNumber(c)) {
                        map.upgrade(c);
                        state = 22;
                    } else if (c == ':') {
                        map.convert(Constants.DT_YY, Constants.DT_HH);
                        state = 4;
                    } else {
                        i = i - 1;
                        state = -1;
                    }
                    break;
                case 22:
                    if (Util.isNumber(c)) {
                        map.append(c);
                        state = -1;
                    } else {
                        map.remove(Constants.DT_YYYY);//since there is no three number year
                        i = i - 1;
                        state = -1;
                    }
                    break;
                case 24:
                    if (Util.isDateOperator(c) || c == Constants.CH_COMA) {
                        delimeterStack.push(c);
                        state = 24;
                    }
                    else if (Util.isNumber(c)) {
                        map.upgrade(c);
                        state = 20;
                    } else {
                        i = i - 1;
                        state = -1;
                    }
                    break;
                case 25://potential year start comes here
                    if (Util.isNumber(c)) {
                        map.setType(Constants.TY_DTE, Constants.DT_YYYY);
                        map.put(Constants.DT_MM, c);
                        state = 26;
                    } else if (i > 0 && (p = Util.checkTypes("FSA_TIMES", str.substring(i))) != null) {
                        map.setType(Constants.TY_TME, null);
                        String s = str.substring(0, i);
                        if (p.getB().equals("mins"))
                            s = "00" + s;
                        extractTime(s, map.getValMap());
                        i = i + p.getA();
                        state = -1;
                    } else {
                        //it wasn't year, it was just a number
                        i = i - 2;
                        state = -1;
                    }
                    break;
                case 26:
                    if (Util.isNumber(c)) {
                        map.append(c);
                        state = 27;
                    } else {
                        map.setType(Constants.TY_STR, Constants.TY_STR);
                        i = i - 1;
                        state = -1;
                    }
                    break;
                case 27:
                    if (Util.isDateOperator(c)) {
                        delimeterStack.push(c);
                        state = 28;
                    }
                    else if (Util.isNumber(c)) {//it was a number, most prbly tele-num
                        if (map.getType().equals(Constants.TY_DTE)) {
                            map.setType(Constants.TY_NUM, Constants.TY_NUM);
                        }
                        map.append(c);
                        if ((delimeterStack.pop() == Constants.CH_SLSH || delimeterStack.pop() == Constants.CH_HYPH) && i + 1 < str.length() && Util.isNumber(str.charAt(i + 1)) && (i + 2 == str.length() || Util.isDelimeter(str.charAt(i + 2)))) {//flight time 0820/0950
                            map.setType(Constants.TY_TMS, Constants.TY_TMS);
                            map.append(str.charAt(i + 1));
                            i = i + 1;
                            state = -1;
                        } else if (delimeterStack.pop() == Constants.CH_SPACE) {
                            state = 41;
                        } else
                            state = 12;
                    } else if (c == 42 || c == 88 || c == 120) {//*Xx
                        map.setType(Constants.TY_ACC, Constants.TY_ACC);
                        map.append('X');
                        state = 11;
                    } else {
                        map.setType(Constants.TY_STR, Constants.TY_STR);
                        i = i - 1;
                        state = -1;
                    }
                    break;
                case 28:
                    if (Util.isNumber(c)) {
                        map.put(Constants.DT_D, c);
                        state = 29;
                    } else {
                        map.setType(Constants.TY_STR, Constants.TY_STR);
                        i = i - 2;
                        state = -1;
                    }
                    break;
                case 29:
                    if (Util.isNumber(c)) {
                        map.append(c);
                    } else
                        i = i - 1;
                    state = -1;
                    break;
                case 30:
                    if (c == Constants.CH_COMA || c == Constants.CH_SPACE)
                        state = 30;
                    else if (Util.isNumber(c)) {
                        map.put(Constants.DT_D, c);
                        state = 31;
                    } else {
                        map.setType(Constants.TY_DTE);
                        i = i - 1;
                        state = -1;
                    }
                    break;
                case 31:
                    if (Util.isNumber(c)) {
                        map.append(c);
                        state = 32;
                    } else if ((p = Util.checkTypes("FSA_MONTHS", str.substring(i))) != null) {
                        map.put(Constants.DT_MMM, p.getB());
                        i += p.getA();
                        state = 24;
                    } else if (c == Constants.CH_COMA || c == Constants.CH_SPACE)
                        state = 32;
                    else {
                        i = i - 1;
                        state = -1;
                    }
                    break;
                case 32:
                    if ((p = Util.checkTypes("FSA_MONTHS", str.substring(i))) != null) {
                        map.put(Constants.DT_MMM, p.getB());
                        i += p.getA();
                        state = 24;
                    } else if (c == Constants.CH_COMA || c == Constants.CH_SPACE)
                        state = 32;
                    else if ((p = Util.checkTypes("FSA_DAYSFFX", str.substring(i))) != null) {
                        i += p.getA();
                        state = 32;
                    } else {
                        int j = i;
                        while (!Util.isNumber(str.charAt(j)))
                            j--;
                        i = j;
                        state = -1;
                    }
                    break;
                case 33:
                    if (Util.isNumber(c)) {
                        map.put(Constants.DT_D, c);
                        state = 34;
                    } else if (c == Constants.CH_SPACE || c == Constants.CH_COMA || c == Constants.CH_HYPH)
                        state = 33;
                    else {
                        map.setType(Constants.TY_DTE);
                        i = i - 1;
                        state = -1;
                    }
                    break;
                case 34:
                    if (Util.isNumber(c)) {
                        map.append(c);
                        state = 35;
                    } else if (c == Constants.CH_SPACE || c == Constants.CH_COMA)
                        state = 35;
                    else {
                        map.setType(Constants.TY_DTE);
                        i = i - 1;
                        state = -1;
                    }
                    break;
                case 35:
                    if (Util.isNumber(c)) {
                        map.convert(Constants.DT_D, Constants.DT_YYYY);
                        map.append(c);
                        state = 20;
                    } else if (c == Constants.CH_SPACE || c == Constants.CH_COMA)
                        state = 40;
                    else {
                        map.setType(Constants.TY_DTE);
                        i = i - 1;
                        state = -1;
                    }
                    break;
                case 36:
                    if (Util.isNumber(c)) {
                        map.append(c);
                        counter++;
                    } else {
                        if (counter == 12)
                            map.setType(Constants.TY_NUM, Constants.TY_NUM);
                        else
                            return null;
                        state = -1;
                    }
                    break;
                case 37:
                    if (Util.isNumber(c)) {
                        map.setType(Constants.TY_AMT, Constants.TY_AMT);
                        map.put(Constants.TY_AMT, '-');
                        map.append(c);
                        state = 12;
                    } else if (c == Constants.CH_FSTP) {
                        map.put(Constants.TY_AMT, '-');
                        map.append(c);
                        state = 10;
                    } else
                        state = -1;
                    break;
                case 38:
                    i = map.getIndex();
                    state = -1;
                    break;
                case 39://instrno
                    if (Util.isNumber(c))
                        map.append(c);
                    else {
                        map.setType(Constants.TY_ACC, Constants.TY_ACC);
                        state = -1;
                    }
                    break;
                case 40:
                    if (Util.isNumber(c)) {
                        map.put(Constants.DT_YY, c);
                        state = 20;
                    } else if (c == Constants.CH_SPACE || c == Constants.CH_COMA)
                        state = 40;
                    else {
                        map.setType(Constants.TY_DTE);
                        i = i - 1;
                        state = -1;
                    }
                    break;
                case 41://for phone numbers; same as 12 + space; coming from 27
                    if (Util.isNumber(c)) {
                        map.append(c);
                    } else if (c == Constants.CH_SPACE)
                        state = 41;
                    else {
                        if ((i - 1) > 0 && str.charAt(i - 1) == Constants.CH_SPACE)
                            i = i - 2;
                        else
                            i = i - 1;
                        state = -1;
                    }
                    break;
                case 42: //18->12 case, where 7-2209 was becoming amt as part of phn support
                    if (Util.isNumber(c)) {
                        map.append(c);
                    } else if (c == Constants.CH_HYPH && (i + 1) < str.length() && Util.isNumber(str.charAt(i + 1))) {
                        state = 39;
                    } else {
                        i = i - 1;
                        state = -1;
                    }
                    break;
            }
            i++;
            if (D_DEBUG) {
                L.msg("ch:" + c + " state:" + state + " map:" + map.print());
            }
        }
        if (map.getType() == null)
            return null;
        //sentence end cases
        if (state == 10) {
            map.pop();
            i = i - 1;
        } else if (state == 36) {
            if (counter == 12)
                map.setType(Constants.TY_NUM, Constants.TY_NUM);
            else
                return null;
        }

        if (map.getType().equals(Constants.TY_AMT)) {
            if (!map.contains(map.getType()) || ((map.get(map.getType()).contains(".") && map.get(map.getType()).split("\\.")[0].length() > 8) || (!map.get(map.getType()).contains(".") && map.get(map.getType()).length() > 8)))
                map.setType(Constants.TY_NUM, Constants.TY_NUM);
        }

        if (map.getType().equals(Constants.TY_NUM)) {
            if (i < str.length() && Character.isAlphabetic(str.charAt(i)) && !config.containsKey(Constants.YUGA_SOURCE_CONTEXT)) {
                int j = i;
                while (j < str.length() && str.charAt(j) != ' ')
                    j++;
                map.setType(Constants.TY_STR, Constants.TY_STR);
                i = j;
            } else if (map.get(Constants.TY_NUM) != null) {
                if (map.get(Constants.TY_NUM).length() == 10 && (map.get(Constants.TY_NUM).charAt(0) == '9' || map.get(Constants.TY_NUM).charAt(0) == '8' || map.get(Constants.TY_NUM).charAt(0) == '7'))
                    map.setVal("num_class", Constants.TY_PHN);
                else if (map.get(Constants.TY_NUM).length() == 12 && map.get(Constants.TY_NUM).startsWith("91"))
                    map.setVal("num_class", Constants.TY_PHN);
                else if (map.get(Constants.TY_NUM).length() == 11 && map.get(Constants.TY_NUM).startsWith("18"))
                    map.setVal("num_class", Constants.TY_PHN);
                else if (map.get(Constants.TY_NUM).length() == 11 && map.get(Constants.TY_NUM).charAt(0) == '0')
                    map.setVal("num_class", Constants.TY_PHN);
                else
                    map.setVal("num_class", Constants.TY_NUM);
            }
        } else if (map.getType().equals(Constants.TY_DTE) && (i + 1) < str.length()) {
            Pair<Integer, String> pTime;
            int in = i + skip(str.substring(i));
            if (in < str.length()) {
                if (Util.isNumber(str.charAt(in)) || Util.checkTypes("FSA_MONTHS", str.substring(in)) != null || Util.checkTypes("FSA_DAYS", str.substring(in)) != null) {
                    Pair<Integer, FsaContextMap> p_ = parseInternal(str.substring(in), config);
                    if (p_ != null && p_.getB().getType().equals(Constants.TY_DTE)) {
                        map.putAll(p_.getB());
                        i = in + p_.getA();
                    }
                } else if ((pTime = Util.checkTypes("FSA_TIMEPRFX", str.substring(in))) != null) {
                    int iTime = in + pTime.getA() + 1 + skip(str.substring(in + pTime.getA() + 1));
                    if (iTime < str.length() && (Util.isNumber(str.charAt(iTime)) || Util.checkTypes("FSA_DAYS", str.substring(iTime)) != null)) {
                        Pair<Integer, FsaContextMap> p_ = parseInternal(str.substring(iTime), config);
                        if (p_ != null && p_.getB().getType().equals(Constants.TY_DTE)) {
                            map.putAll(p_.getB());
                            i = iTime + p_.getA();
                        }
                    }
                } else if ((pTime = Util.checkTypes("FSA_TZ", str.substring(in))) != null) {
                    int itz = in + pTime.getA() + nextSpace(str.substring(in + pTime.getA() + 1)) + 2;
                    if ((itz + 3) < str.length() && Util.isNumber(str.charAt(itz)) && Util.isNumber(str.charAt(itz + 1)) && Util.isNumber(str.charAt(itz + 2)) && Util.isNumber(str.charAt(itz + 3))) {
                        map.put(Constants.DT_YYYY, str.substring(itz, itz + 4));
                    }
                }
            }
        } else if (map.getType().equals(Constants.TY_TMS)) {
            String v = map.get(map.getType());
            if (v != null && v.length() == 8 && Util.isHour(v.charAt(0), v.charAt(1)) && Util.isHour(v.charAt(4), v.charAt(5))) {
                extractTime(v.substring(0, 4), map.getValMap(), "dept");
                extractTime(v.substring(4, 8), map.getValMap(), "arrv");
            }
        }
        return new Pair<Integer,FsaContextMap>(i, map);
    }

    private static int skip(String str) {
        int i = 0;
        while (i < str.length()) {
            if (str.charAt(i) == ' ' || str.charAt(i) == ',' || str.charAt(i) == '(' || str.charAt(i) == ':')
                i++;
            else
                break;
        }
        return i;
    }

    private static int nextSpace(String str) {
        int i = 0;
        while (i < str.length()) {
            if (str.charAt(i) == ' ')
                return i;
            else
                i++;
        }
        return i;
    }

    private static int accAmtNumPct(String str, int i, FsaContextMap map, HashMap<String, String> config) {
        //acc num amt pct
        Pair<Integer, String> p;
        char c = str.charAt(i);
        String subStr = str.substring(i);
        if (c == Constants.CH_FSTP) { //dot
            map.append(c);
            return 10;
        } else if (c == 42 || c == 88 || c == 120) {//*Xx
            map.setType(Constants.TY_ACC, Constants.TY_ACC);
            map.append('X');
            return 11;
        } else if (c == Constants.CH_COMA) { //comma
            return 12;
        } else if (c == Constants.CH_PCT || (c == Constants.CH_SPACE && (i + 1) < str.length() && str.charAt(i + 1) == Constants.CH_PCT)) { //pct
            map.setType(Constants.TY_PCT, Constants.TY_PCT);
            return -1;
        } else if (c == Constants.CH_PLUS) {
            if (config.containsKey(Constants.YUGA_SOURCE_CONTEXT) && config.get(Constants.YUGA_SOURCE_CONTEXT).equals(Constants.YUGA_SC_CURR)) {
                return -1;
            }
            map.setType(Constants.TY_STR, Constants.TY_STR);
            return 36;
        } else if (i > 0 && (p = Util.checkTypes("FSA_AMT", subStr)) != null) {
            map.setIndex(p.getA());
            map.setType(Constants.TY_AMT, Constants.TY_AMT);
            map.append(getAmt(p.getB()));
            return 38;
        } else if (i > 0 && (p = Util.checkTypes("FSA_TIMES", subStr)) != null) {
            int ind = i + p.getA();
            map.setIndex(ind);
            map.setType(Constants.TY_TME, null);
            String s = str.substring(0, i);
            if (p.getB().equals("mins"))
                s = "00" + s;
            extractTime(s, map.getValMap());
            return 38;
        } else
            return -1;
    }

    private static String getAmt(String type) {
        switch (type) {
            case "lakh":
            case "lac":
                return "00000";
            case "k":
                return "000";
            default:
                return "";
        }
    }

    private static void extractTime(String str, HashMap<String, String> valMap, String... prefix) {
        String pre = "";
        if (prefix != null && prefix.length > 0)
            pre = prefix[0] + "_";
        Pattern pattern = Pattern.compile("([0-9]{2})([0-9]{2})?([0-9]{2})?");
        Matcher m = pattern.matcher(str);
        if (m.find()) {
            valMap.put(pre + "time", m.group(1) + ((m.groupCount() > 1 && m.group(2) != null) ? ":" + m.group(2) : ":00"));
        }
    }

    static class DelimeterStack {
        ArrayList<Character> stack;

        public DelimeterStack() {
            stack = new ArrayList<Character>();
        }

        void push(char ch) {
            stack.add(ch);
        }

        char pop() {
            if (stack.size() > 0)
                return stack.get(stack.size() - 1);
            return '~';
        }
    }

}
