/*
 * Aipo is a groupware program developed by Aimluck,Inc.
 * Copyright (C) 2004-2011 Aimluck,Inc.
 * http://www.aipo.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.aimluck.eip.mail.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.commons.codec.binary.Base64;
import org.apache.jetspeed.services.JetspeedSecurity;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.jetspeed.services.resources.JetspeedResources;
import org.apache.turbine.services.upload.TurbineUpload;
import org.apache.turbine.util.RunData;

import com.aimluck.commons.utils.ALStringUtil;
import com.aimluck.eip.cayenne.om.account.EipMCompany;
import com.aimluck.eip.cayenne.om.portlet.EipMMailAccount;
import com.aimluck.eip.cayenne.om.portlet.EipMMailNotifyConf;
import com.aimluck.eip.cayenne.om.portlet.EipTMail;
import com.aimluck.eip.cayenne.om.portlet.EipTMailFilter;
import com.aimluck.eip.cayenne.om.portlet.EipTMailFolder;
import com.aimluck.eip.cayenne.om.security.TurbineUser;
import com.aimluck.eip.common.ALBaseUser;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.common.ALEipUser;
import com.aimluck.eip.mail.ALLocalMailMessage;
import com.aimluck.eip.mail.ALMailFactoryService;
import com.aimluck.eip.mail.ALMailHandler;
import com.aimluck.eip.mail.ALMailMessage;
import com.aimluck.eip.mail.ALMailReceiverContext;
import com.aimluck.eip.mail.ALMailSenderContext;
import com.aimluck.eip.mail.ALMailService;
import com.aimluck.eip.mail.ALPop3MailReceiverContext;
import com.aimluck.eip.mail.ALSmtpMailContext;
import com.aimluck.eip.mail.ALSmtpMailSender;
import com.aimluck.eip.mail.ALSmtpMailSenderContext;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.services.eventlog.ALEventlogConstants;
import com.aimluck.eip.services.eventlog.ALEventlogFactoryService;
import com.aimluck.eip.services.storage.ALStorageService;
import com.aimluck.eip.util.ALEipUtils;
import com.aimluck.eip.util.ALServletUtils;
import com.sk_jp.mail.JISDataSource;
import com.sk_jp.mail.MailUtility;

/**
 * メールのユーティリティクラスです。 <BR>
 * 
 */
public class ALMailUtils {

  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(ALMailUtils.class.getName());

  /** 改行文字 */
  public static final String CR = System.getProperty("line.separator");

  /** アカウントタイプを無指定 */
  public static final int ACCOUNT_TYPE_NON = 0;

  /** デフォルトアカウント */
  public static final int ACCOUNT_TYPE_DEFAULT = 1;

  /** 初期アカウント */
  public static final int ACCOUNT_TYPE_INIT = 2;

  /** メール通知のキー（毎日通知スケジュール用） */
  public static final int KEY_MSGTYPE_DAYMAIL = 1;

  /** メール通知のキー（伝言メモ用） */
  public static final int KEY_MSGTYPE_NOTE = 21;

  /** メール通知のキー（ブログ用） */
  public static final int KEY_MSGTYPE_BLOG = 22;

  /** メール通知のキー（ワークフロー用） */
  public static final int KEY_MSGTYPE_WORKFLOW = 23;

  /** メール通知のキー（スケジュール用） */
  public static final int KEY_MSGTYPE_SCHEDULE = 24;

  /** メール通知のキー（ToDo用） */
  public static final int KEY_MSGTYPE_TODO = 25;

  /** メール通知のキー（報告書用） */
  public static final int KEY_MSGTYPE_REPORT = 26;

  /** メール通知のキー（掲示板用） */
  public static final int KEY_MSGTYPE_MSGBOARD = 27;

  /** メール通知のキー（報告書用） */
  public static final int KEY_MSGTYPE_PROJECT = 28;

  /** メール通知の値（送信先なし） */
  public static final int VALUE_MSGTYPE_DEST_NONE = 0;

  /** メール通知の値（パソコン用メールアドレス） */
  public static final int VALUE_MSGTYPE_DEST_PC = 1;

  /** メール通知の値（携帯用メールアドレス） */
  public static final int VALUE_MSGTYPE_DEST_CELLULAR = 2;

  /** メール通知の値（パソコン用メールアドレスと携帯用メールアドレス） */
  public static final int VALUE_MSGTYPE_DEST_PC_CELLULAR = 3;

  /** オブジェクト比較タイプ：昇順 */
  public static final int COMPARE_TYPE_ASC = 1;

  /** オブジェクト比較タイプ：降順 */
  public static final int COMPARE_TYPE_DESC = 2;

  /** オブジェクト比較名称：件名 */
  public static final int COMPARE_NAME_SUBJECT = 1;

  /** オブジェクト比較名称：差出人 or 受取人 */
  public static final int COMPARE_NAME_PERSON = 2;

  /** オブジェクト比較名称：日付 */
  public static final int COMPARE_NAME_DATE = 3;

  /** オブジェクト比較名称：ファイル容量 */
  public static final int COMPARE_NAME_FILE_VOLUME = 4;

  public static final String DATE_FORMAT = "yyyy/MM/dd HH:mm";

  public static final String storePath = JetspeedResources.getString(
    "aipo.home",
    "");

  public static final String rootFolderPath = JetspeedResources.getString(
    "aipo.mail.home",
    "");

  public static final String categoryKey = JetspeedResources.getString(
    "aipo.mail.key",
    "");

  /** メールアカウントのパスワードを暗号化する時の共通鍵 */
  private static final String seacretPassword = "1t's a s3@cr3t k3y";

  /** フィルタタイプ */
  /** 送信元メールアドレス */
  public final static String FILTER_TYPE_MAILADDRESS = "M";

  /** 送信元ドメイン */
  public final static String FILTER_TYPE_DOMAIN = "D";

  /** 件名 */
  public final static String FILTER_TYPE_SUBJECT = "S";

  /** 送信先メールアドレス */
  public final static String FILTER_TYPE_TO = "T";

  /** セッションの識別子 */
  public static final String FOLDER_ID = "folderid";

  /**
   * メールアカウントを取得する．
   * 
   * @param userId
   * @param accountId
   * @return
   */
  public static EipMMailAccount getMailAccount(int userId, int accountId) {
    if (userId < 0 || accountId < 0) {
      return null;
    }

    try {

      SelectQuery<EipMMailAccount> query =
        Database.query(EipMMailAccount.class);
      Expression exp1 =
        ExpressionFactory.matchExp(EipMMailAccount.USER_ID_PROPERTY, Integer
          .valueOf(userId));
      Expression exp2 =
        ExpressionFactory.matchDbExp(
          EipMMailAccount.ACCOUNT_ID_PK_COLUMN,
          Integer.valueOf(accountId));

      EipMMailAccount account =
        query.andQualifier(exp1).andQualifier(exp2).fetchSingle();
      if (account == null) {
        logger.debug("[WebMail] Not found AccountID...");
        return null;
      }
      return account;
    } catch (Exception ex) {
      logger.error("ALMailUtils.getMailAccount", ex);
      return null;
    }
  }

  /**
   * メールアカウント一覧を取得する．
   * 
   * @param userId
   * @param accountId
   * @return
   */
  public static List<EipMMailAccount> getMailAccountList(int userId) {
    if (userId < 0) {
      return null;
    }

    try {

      SelectQuery<EipMMailAccount> query =
        Database.query(EipMMailAccount.class);
      Expression exp1 =
        ExpressionFactory.matchExp(EipMMailAccount.USER_ID_PROPERTY, Integer
          .valueOf(userId));

      List<EipMMailAccount> fetchList = query.andQualifier(exp1).fetchList();

      if (fetchList == null || fetchList.size() <= 0) {
        logger.debug("[WebMail] Not found AccountID...");
        return null;
      }
      return fetchList;
    } catch (Exception ex) {
      logger.error("ALMailUtils.getMailAccountList", ex);
      return null;
    }
  }

  /**
   * メールアカウント名を取得する．
   * 
   * @param userId
   * @param accountId
   * @return
   */
  public static String getAccountName(int userId, int accountId) {
    if (userId < 0 || accountId < 0) {
      return null;
    }

    String accountName = null;
    try {
      EipMMailAccount mailAccount = getMailAccount(userId, accountId);
      if (mailAccount == null) {
        return null;
      }
      accountName = mailAccount.getAccountName();
    } catch (Exception ex) {
      logger.error("ALMailUtils.getAccountName", ex);
      return null;
    }
    return accountName;
  }

  /**
   * メールの返信に必要な値をセットする．
   * 
   * @param msg
   * @return
   */
  public static ALMailMessage getReplyMessage(ALMailMessage mailmsg) {
    if (mailmsg == null) {
      return null;
    }

    ALLocalMailMessage msg = null;
    try {
      msg = (ALLocalMailMessage) mailmsg;

      StringBuffer sb = new StringBuffer();
      sb.append("From: ").append(getFromDelegate(msg)).append(CR);
      sb
        .append("To: ")
        .append(
          getAddressString(msg.getRecipients(Message.RecipientType.TO, false)))
        .append(CR);
      sb.append("Sent: ").append(msg.getSentDate()).append(CR);
      sb
        .append("Subject: ")
        .append(UnicodeCorrecter.correctToISO2022JP(msg.getSubject()))
        .append(CR)
        .append(" ")
        .append(CR);

      msg.setSubject(MimeUtility.encodeText(UnicodeCorrecter
        .correctToISO2022JP(msg.getSubject())));
      msg
        .setRecipient(Message.RecipientType.TO, getReplyToDelegateExtract(msg));
      String[] lines = msg.getBodyTextArray();
      if (lines != null && lines.length > 0) {
        int length = lines.length;
        for (int i = 0; i < length; i++) {
          sb.append(lines[i]).append(CR);
        }
      }
      msg.setText(UnicodeCorrecter.correctToISO2022JP(sb.toString()));
    } catch (Exception e) {
      logger.error("ALMailUtils.getReplyMessage", e);
      return null;
    }
    return msg;
  }

  /**
   * メールの返信に必要な値をセットする．
   * 
   * @param msg
   * @return
   */
  public static ALMailMessage getReplyAllMessage(ALMailMessage mailmsg) {
    if (mailmsg == null) {
      return null;
    }

    ALLocalMailMessage msg = null;
    try {
      msg = (ALLocalMailMessage) mailmsg;

      StringBuffer sb = new StringBuffer();
      sb.append("From: ").append(getFromDelegate(msg)).append(CR);
      sb
        .append("To: ")
        .append(
          getAddressString(msg.getRecipients(Message.RecipientType.TO, false)))
        .append(CR);
      sb.append("Sent: ").append(msg.getSentDate()).append(CR);
      sb
        .append("Subject: ")
        .append(UnicodeCorrecter.correctToISO2022JP(msg.getSubject()))
        .append(CR)
        .append(" ")
        .append(CR);

      msg.setSubject(MimeUtility.encodeText(UnicodeCorrecter
        .correctToISO2022JP(msg.getSubject())));
      String[] lines = msg.getBodyTextArray();
      if (lines != null && lines.length > 0) {
        int length = lines.length;
        for (int i = 0; i < length; i++) {
          sb.append(lines[i]).append(CR);
        }
      }
      msg.setText(UnicodeCorrecter.correctToISO2022JP(sb.toString()));
    } catch (Exception e) {
      logger.error("ALMailUtils.getReplyMessage", e);
      return null;
    }
    return msg;
  }

  /**
   * メールの転送に必要な値をセットする．
   * 
   * @param msg
   * @return
   */
  public static ALMailMessage getForwardMessage(ALMailMessage mailmsg) {
    if (mailmsg == null) {
      return null;
    }

    ALLocalMailMessage msg = null;
    try {
      msg = (ALLocalMailMessage) mailmsg;

      StringBuffer sb = new StringBuffer();
      sb.append("From: ").append(getFromDelegate(msg)).append(CR);
      sb
        .append("To: ")
        .append(
          getAddressString(msg.getRecipients(Message.RecipientType.TO, false)))
        .append(CR);
      sb.append("Sent: ").append(msg.getSentDate()).append(CR);
      sb
        .append("Subject: ")
        .append(UnicodeCorrecter.correctToISO2022JP(msg.getSubject()))
        .append(CR)
        .append(" ")
        .append(CR);

      msg.setSubject(MimeUtility.encodeText(UnicodeCorrecter
        .correctToISO2022JP(msg.getSubject())));
      // msg.setRecipient(Message.RecipientType.TO, msg.getFrom()[0]);
      String[] lines = msg.getBodyTextArray();
      if (lines != null && lines.length > 0) {
        int length = lines.length;
        for (int i = 0; i < length; i++) {
          sb.append(lines[i]).append(CR);
        }
      }
      msg.setText(UnicodeCorrecter.correctToISO2022JP(sb.toString()));
    } catch (Exception e) {
      logger.error("ALMailUtils.getForwardMessage", e);
      return null;
    }
    return msg;
  }

  /**
   * 改行を含む文字列を，改行で区切った文字列の配列を取得する．
   * 
   * @param str
   * @return
   */
  public static String[] getLines(String str) {
    if (str == null || str.equals("")) {
      return null;
    }
    if (str.indexOf(CR) < 0) {
      return new String[] { str };
    }

    String token = null;
    List<String> tokens = new ArrayList<String>();
    BufferedReader reader = null;
    String[] lines = null;
    try {
      reader = new BufferedReader(new StringReader(str));
      while ((token = reader.readLine()) != null) {
        tokens.add(token);
      }
      reader.close();

      lines = new String[tokens.size()];
      lines = tokens.toArray(lines);
    } catch (Exception ioe) {
      logger.error("ALMailUtils.getLines", ioe);
      try {
        reader.close();
      } catch (IOException e) {
      }
      return null;
    }
    return lines;
  }

  /**
   * 区切り文字で区切った文字列の配列を取得する．
   * 
   * @param line
   *          区切り文字を含む文字列
   * @param delim
   *          区切り文字
   * @return
   */
  public static String[] getTokens(String line, String delim) {
    if (line == null || line.equals("")) {
      return null;
    }
    if (line.indexOf(delim) < 0) {
      return new String[] { line };
    }

    StringTokenizer st = new StringTokenizer(line, delim);
    int length = st.countTokens();
    String[] tokens = new String[length];
    for (int i = 0; i < length; i++) {
      tokens[i] = st.nextToken();
    }
    return tokens;
  }

  /**
   * 指定された配列の並び順を逆にする．
   * 
   * @param objs
   * @return
   */
  public static int[] reverse(int[] objs) {
    if (objs == null) {
      return null;
    }
    int length = objs.length;
    int[] destObjs = new int[length];
    System.arraycopy(objs, 0, destObjs, 0, length);
    Arrays.sort(destObjs);

    int[] reverseObjs = new int[length];
    for (int i = 0; i < length; i++) {
      reverseObjs[i] = destObjs[length - i - 1];
    }
    return reverseObjs;

  }

  /**
   * 複数のアドレスをカンマ区切りの1つの文字列に変換する．
   * 
   * @param addresses
   * @return
   */
  public static String getAddressString(Address[] addresses) {
    if (addresses == null || addresses.length <= 0) {
      return "";
    }
    HashSet<String> foundAddress = new HashSet<String>();

    StringBuffer sb = new StringBuffer();
    InternetAddress addr = null;
    int length = addresses.length;
    for (int i = 0; i < length; i++) {
      addr = (InternetAddress) addresses[i];
      if (foundAddress.contains(addr.getAddress())) {
        continue;
      }
      foundAddress.add(addr.getAddress());

      if (addr.getPersonal() != null) {
        String personaladdr =
          getOneString(getTokens(addr.getPersonal(), "\r\n"), "");
        sb.append(personaladdr).append(" <").append(addr.getAddress()).append(
          ">, ");
      } else {
        sb.append(addr.getAddress()).append(", ");
      }
    }
    String addressStr = sb.toString();
    return addressStr.substring(0, addressStr.length() - 2);
  }

  /**
   * 複数の文字列を区切り文字で区切った1つの文字列に変換する．
   * 
   * @param addresses
   * @param delim
   * @return
   */
  public static String getOneString(String[] strs, String delim) {
    if (strs == null) {
      return "";
    }
    String delimiter = delim + " ";
    StringBuffer sb = new StringBuffer();
    int length = strs.length - 1;
    for (int i = 0; i < length; i++) {
      sb.append(strs[i]).append(delimiter);
    }
    sb.append(strs[length]);
    return sb.toString();
  }

  /**
   * Date のオブジェクトを "yyyy/MM/dd hh:mm" 形式の文字列に変換する．
   * 
   * @param date
   * @return
   */
  public static String translateDate(Date date) {
    if (date == null) {
      return null;
    }

    // 日付を表示形式に変換
    SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
    sdf.setTimeZone(TimeZone.getTimeZone("JST"));
    return sdf.format(date);
  }

  /**
   * "yyyy/MM/dd hh:mm" 形式の文字列を Date のオブジェクトに変換する．
   * 
   * @param dateStr
   * @return
   */
  public static Date translateDate(String dateStr) {
    if (dateStr == null || dateStr.equals("")) {
      return null;
    }
    Date date = null;

    // 日付を表示形式に変換
    SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
    sdf.setTimeZone(TimeZone.getTimeZone("JST"));
    try {
      date = sdf.parse(dateStr);
    } catch (Exception e) {
      return null;
    }
    return date;
  }

  /**
   * 受信メールの受信日時を取得する（ヘッダ Recieved を解析する）．
   * 
   * @param msg
   * @return
   */
  public static Date getReceivedDate(MimeMessage msg) {
    try {
      String[] receivedHeaders = msg.getHeader("Received");

      if (receivedHeaders == null || receivedHeaders.length == 0) {
        return null;
      }

      Date receivedDate = null;
      StringTokenizer st = new StringTokenizer(receivedHeaders[0], ";");
      if (st.countTokens() == 2) {
        st.nextToken();
        String receivedDateStr = st.nextToken();
        if (receivedDateStr != null && !receivedDateStr.equals("")) {
          receivedDate = MailUtility.parseDate(receivedDateStr);
        }
        return receivedDate;
      } else {
        return null;
      }
    } catch (Exception e) {
      return null;
    }
  }

  public static String convertBase64ToIso2022(String str) {
    if (str == null || str.length() <= 0) {
      return str;
    }

    StringBuffer sb = new StringBuffer("=?ISO-2022-JP?B?");
    sb.append(str).append("?=");

    return sb.toString();
  }

  /**
   * 日本語を含むヘッダ用テキストを生成します。 変換結果は ASCII なので、これをそのまま setSubject や InternetAddress
   * のパラメタとして使用してください。
   */
  public static String encodeWordJIS(String s) {
    try {
      return "=?ISO-2022-JP?B?"
        + new String(Base64.encodeBase64(s.getBytes("ISO-2022-JP")))
        + "?=";
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("CANT HAPPEN");
    }
  }

  /**
   * String 型のアドレス → InternetAddress 型のアドレス に変換する．
   * 
   * @param addr
   * @return
   */
  public static InternetAddress getInternetAddress(String addr) {
    InternetAddress address = null;
    StringTokenizer st = new StringTokenizer(addr, "<>");
    int count = st.countTokens();
    try {
      if (count <= 0) {
        return null;
      } else if (count == 1) {
        address = new InternetAddress(st.nextToken().trim());
      } else if (count == 2) {
        String name = st.nextToken().trim();
        String addressStr = st.nextToken().trim();
        address =
          new InternetAddress(addressStr, ALMailUtils.encodeWordJIS(name));
      } else if (count > 2) {
        String name = "";
        String addressStr = "";
        while (st.hasMoreTokens()) {
          String tmp = st.nextToken().trim();
          if (ALStringUtil.isMailAddress(tmp)) {
            addressStr = tmp;
          }
        }

        if (!"".equals(addressStr)) {
          String tmpName = addr.replace("<" + addressStr + ">", "").trim();
          if (!("\"".equals(tmpName.substring(0, 1)) && "\"".equals(tmpName
            .substring(tmpName.length() - 1, tmpName.length())))) {
            name = "\"" + tmpName + "\"";
          } else {
            name = tmpName;
          }
        }

        address =
          new InternetAddress(addressStr, ALMailUtils.encodeWordJIS(name));
      }
    } catch (Exception e) {
      logger.error("ALMailUtils.getInternetAddress", e);
      return null;
    }
    return address;
  }

  public static String getFileNameFromText(String filePath) {
    String line = "";
    BufferedReader reader = null;
    try {
      reader =
        new BufferedReader(new InputStreamReader(ALStorageService
          .getFile(filePath + ".txt"), ALEipConstants.DEF_CONTENT_ENCODING));
      line = reader.readLine();
    } catch (Exception e) {
      logger.error("ALMailUtils.getFileNameFromText", e);
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException ex) {
          logger.error("ALMailUtils.getFileNameFromText", ex);
        }
      }
    }
    return line;

  }

  /**
   * テキストをセットします。 Part#setText() の代わりにこちらを使うようにします。
   */
  public static void setTextContent(Part p, String s) throws MessagingException {
    p.setDataHandler(new DataHandler(new JISDataSource(s)));
    p.setHeader(ALLocalMailMessage.CONTENT_TRANSFER_ENCORDING, "7bit");
  }

  /**
   * 「暗号化＋Base64符号化」の文字列を，もとの文字列に復号する．
   * 
   * @param password
   * @param data
   * @return
   */
  public static final byte[] getDecryptedMailAccountPasswd(byte[] data) {
    return getDecryptedMailAccountPasswd(seacretPassword.toCharArray(), data);
  }

  /**
   * 「暗号化＋Base64符号化」の文字列を，もとの文字列に復号する．
   * 
   * @param password
   * @param data
   * @return
   */
  public static final byte[] getDecryptedMailAccountPasswd(char[] password,
      byte[] data) {
    if (data == null) {
      return null;
    }

    byte[] decryptedData = null;
    try {
      decryptedData =
        cryptPBEWithMD5AndDES(Cipher.DECRYPT_MODE, password, data);
      if (decryptedData == null) {
        return null;
      }
    } catch (Exception e) {
      logger.error("ALMailUtils.getDecryptedMailAccountPasswd", e);
      return null;
    }
    return decryptedData;
  }

  /**
   * 「暗号化＋Base64符号化」した文字列を取得する．
   * 
   * @param password
   * @param data
   * @return
   */
  public static final byte[] getEncryptedMailAccountPasswd(byte[] data) {
    return getEncryptedMailAccountPasswd(seacretPassword.toCharArray(), data);
  }

  /**
   * 「暗号化＋Base64符号化」した文字列を取得する．
   * 
   * @param password
   * @param data
   * @return
   */
  public static final byte[] getEncryptedMailAccountPasswd(char[] password,
      byte[] data) {
    if (data == null) {
      return null;
    }

    byte[] encryptedData = null;
    try {
      encryptedData =
        cryptPBEWithMD5AndDES(Cipher.ENCRYPT_MODE, password, data);
    } catch (Exception e) {
      logger.error("ALMailUtils.getEncryptedMailAccountPasswd", e);
      return null;
    }
    return encryptedData;
  }

  /**
   * 指定したパスワードでデータを暗号化／復号する． 暗号化方式：PBEWithMD5AndDES
   * 
   * @param cipherMode
   *          Cipher.ENCRYPT_MODE もしくは Cipher.DECRYPT_MODE
   * @param password
   * @param data
   * @return
   */
  public static final byte[] cryptPBEWithMD5AndDES(int cipherMode,
      char[] password, byte[] data) {
    byte[] ciphertext = null;
    PBEKeySpec pbeKeySpec;
    PBEParameterSpec pbeParamSpec;
    SecretKeyFactory keyFac;

    // Salt
    byte[] salt =
      {
        (byte) 0xc7,
        (byte) 0x73,
        (byte) 0x21,
        (byte) 0x8c,
        (byte) 0x7e,
        (byte) 0xc8,
        (byte) 0xee,
        (byte) 0x99 };

    // Iteration count
    int count = 20;

    // Create PBE parameter set
    pbeParamSpec = new PBEParameterSpec(salt, count);

    pbeKeySpec = new PBEKeySpec(password);
    try {
      keyFac = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
      SecretKey pbeKey = keyFac.generateSecret(pbeKeySpec);

      // Create PBE Cipher
      Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");

      // Initialize PBE Cipher with key and parameters
      pbeCipher.init(cipherMode, pbeKey, pbeParamSpec);

      // Encrypt/Decrypt the cleartext
      ciphertext = pbeCipher.doFinal(data);
    } catch (Exception e) {
      logger.error("ALMailUtils.cryptPBEWithMD5AndDES", e);
      return null;
    }
    return ciphertext;
  }

  /**
   * メール受信用コンテキスト（POP3）を取得します。
   * 
   * @param orgId
   * @param account
   * @return
   */
  public static ALMailReceiverContext getALPop3MailReceiverContext(
      String orgId, EipMMailAccount account) {
    if (account == null) {
      return null;
    }

    ALPop3MailReceiverContext rcontext = new ALPop3MailReceiverContext();
    try {
      rcontext.setOrgId(orgId);
      rcontext.setUserId(account.getUserId());
      rcontext.setAccountId(account.getAccountId());
      rcontext.setPop3Host(account.getPop3serverName());
      rcontext.setPop3Port(account.getPop3Port());
      rcontext.setPop3UserId(account.getPop3userName());
      rcontext.setPop3UserPasswd(new String(ALMailUtils
        .getDecryptedMailAccountPasswd(account.getPop3password())));
      rcontext.setDelete(Integer.parseInt(account.getDelAtPop3Flg()) == 1);
      rcontext.setEnableSavingDays(Integer.parseInt(account
        .getDelAtPop3BeforeDaysFlg()) == 1);
      rcontext.setSavingDays(account.getDelAtPop3BeforeDays().intValue());
      rcontext.setDenyReceivedMail(Integer
        .parseInt(account.getNonReceivedFlg()) == 1);
      rcontext.setAuthReceiveFlag(account.getAuthReceiveFlg().intValue());
      rcontext.setEncryptionFlag(account.getPop3EncryptionFlg());
    } catch (Exception e) {
      return null;
    }

    return rcontext;
  }

  /**
   * メール送信用コンテキスト（SMTP）を取得します。
   * 
   * @param orgId
   * @param account
   * @return
   */
  public static ALMailSenderContext getALSmtpMailSenderContext(String orgId,
      EipMMailAccount account) {
    if (account == null) {
      return null;
    }

    ALSmtpMailSenderContext scontext = new ALSmtpMailSenderContext();

    int authSendFlg = 0;
    try {
      authSendFlg = account.getAuthSendFlg();
    } catch (NumberFormatException e) {
      authSendFlg = 0;
    }

    try {
      scontext.setOrgId(orgId);
      scontext.setUserId(account.getUserId().intValue());
      scontext.setAccountId(account.getAccountId().intValue());
      scontext.setSmtpHost(account.getSmtpserverName());
      scontext.setSmtpPort(account.getSmtpPort());
      scontext.setAuthSendFlag(authSendFlg);
      scontext.setAuthSendUserId(account.getAuthSendUserId());
      scontext.setEncryptionFlag(account.getSmtpEncryptionFlg());

      byte[] auth_pass =
        ALMailUtils.getDecryptedMailAccountPasswd(account
          .getAuthSendUserPasswd());
      if (auth_pass != null) {
        scontext.setAuthSendUserPassword(new String(ALMailUtils
          .getDecryptedMailAccountPasswd(account.getAuthSendUserPasswd())));
      } else {
        scontext.setAuthSendUserPassword(null);
      }

      scontext.setPop3Host(account.getPop3serverName());
      scontext.setPop3Port(account.getPop3Port());
      scontext.setPop3UserId(account.getPop3userName());
      scontext.setPop3UserPasswd(new String(ALMailUtils
        .getDecryptedMailAccountPasswd(account.getPop3password())));
      scontext.setPop3EncryptionFlag(account.getSmtpEncryptionFlg());

    } catch (Exception e) {
      return null;
    }
    return scontext;
  }

  /**
   * 送信メッセージ（SMTP）のコンテキストを取得します。
   * 
   * @param orgId
   * @param account
   * @return
   */
  public static ALSmtpMailContext getALSmtpMailContext(String[] to,
      String[] cc, String[] bcc, String from, String name, String subject,
      String msgText, String[] filePaths, Map<String, String> additionalHeaders) {

    ALSmtpMailContext mailcontext = new ALSmtpMailContext();
    mailcontext.setTo(to);
    mailcontext.setCc(cc);
    mailcontext.setBcc(bcc);
    mailcontext.seFrom(from);
    mailcontext.setName(name);
    mailcontext.setSubject(subject);
    mailcontext.setMsgText(msgText);
    mailcontext.setFilePaths(filePaths);
    mailcontext.setAdditionalHeaders(additionalHeaders);

    return mailcontext;
  }

  /**
   * 管理者 admin のメールアカウントのオブジェクトモデルを取得する． <BR>
   * 
   * @param rundata
   * @param context
   * @param isJoin
   *          カテゴリテーブルをJOINするかどうか
   * @return
   */
  public static EipMMailAccount getEipMMailAccountForAdmin() {
    try {
      SelectQuery<EipMMailAccount> query =
        Database.query(EipMMailAccount.class);

      Expression exp =
        ExpressionFactory.matchExp(EipMMailAccount.USER_ID_PROPERTY, Integer
          .valueOf(1));
      EipMMailAccount account = query.andQualifier(exp).fetchSingle();

      if (account == null) {
        logger.debug("[ALMailUtils] Not found AccountID...");
        return null;
      }
      return account;
    } catch (Exception ex) {
      logger.error("ALMailUtils.getEipMMailAccountForAdmin", ex);
      return null;
    }
  }

  /**
   * デフォルトのメールアカウントの情報をデータベースから所得する．
   * 
   * @param userId
   * @return
   */
  public static EipMMailAccount getFirstEipMMailAccount(int userId) {
    try {
      SelectQuery<EipMMailAccount> query =
        Database.query(EipMMailAccount.class);

      Expression exp1 =
        ExpressionFactory.matchExp(EipMMailAccount.USER_ID_PROPERTY, Integer
          .valueOf(userId));
      Expression exp21 =
        ExpressionFactory.matchExp(EipMMailAccount.ACCOUNT_TYPE_PROPERTY, "1");
      Expression exp22 =
        ExpressionFactory.matchExp(EipMMailAccount.ACCOUNT_TYPE_PROPERTY, "2");
      EipMMailAccount account =
        query.andQualifier(exp1).andQualifier(exp21.orExp(exp22)).fetchSingle();

      if (account == null) {
        logger.debug("[ALMailUtils] Not found AccountID...");
        return null;
      }
      return account;
    } catch (Exception ex) {
      logger.error("ALMailUtils.getFirstEipMMailAccount", ex);
      return null;
    }
  }

  /**
   * メールアカウント情報をデータベースに保存する．
   * 
   * @param rundata
   * @param msgList
   * @param userId
   * @param accountName
   * @param accountType
   * @param mailAddress
   * @param mailUserName
   * @param smtpServerName
   * @param smtpPort
   * @param Pop3ServerName
   * @param pop3Port
   * @param pop3UserName
   * @param pop3Password
   * @param authSendFlag
   * @param authSendUserId
   * @param authSendUserPasswd
   * @param authReceiveFlg
   * @param delAtPop3Flg
   * @param delAtPop3BeforeDaysFlg
   * @param delAtPop3BeforeDays
   * @param nonReceivedFlg
   * @param signature
   * @param smtpEncryptionFlg
   * @param pop3EncryptionFlg
   * @return
   */
  public static boolean insertMailAccountData(RunData rundata,
      List<String> msgList, int userId, String accountName, int accountType,
      String mailAddress, String mailUserName, String smtpServerName,
      int smtpPort, String Pop3ServerName, int pop3Port, String pop3UserName,
      String pop3Password, int authSendFlag, String authSendUserId,
      String authSendUserPasswd, int authReceiveFlg, int delAtPop3Flg,
      int delAtPop3BeforeDaysFlg, int delAtPop3BeforeDays,
      String nonReceivedFlg, String signature, int smtpEncryptionFlg,
      int pop3EncryptionFlg) {

    boolean enableUpdate = false;

    try {
      Date createdDate = Calendar.getInstance().getTime();

      EipMMailAccount mailAccount = null;
      EipTMailFolder mailFolder = null;

      if (accountType == ACCOUNT_TYPE_INIT) {
        mailAccount = Database.create(EipMMailAccount.class);
        mailAccount.setAccountType(Integer.toString(ACCOUNT_TYPE_INIT));
      } else {
        SelectQuery<EipMMailAccount> query =
          Database.query(EipMMailAccount.class);

        Expression exp1 =
          ExpressionFactory.matchExp(EipMMailAccount.USER_ID_PROPERTY, Integer
            .valueOf(userId));
        Expression exp2 =
          ExpressionFactory.matchExp(
            EipMMailAccount.ACCOUNT_TYPE_PROPERTY,
            Integer.valueOf(ACCOUNT_TYPE_INIT));
        EipMMailAccount account =
          query.andQualifier(exp1).andQualifier(exp2).fetchSingle();

        if (account == null) {
          // 新規オブジェクトモデル
          mailAccount = Database.create(EipMMailAccount.class);

          SelectQuery<EipMMailAccount> query3 =
            Database.query(EipMMailAccount.class);

          Expression exp3 =
            ExpressionFactory.matchExp(
              EipMMailAccount.USER_ID_PROPERTY,
              Integer.valueOf(userId));
          EipMMailAccount anotherAccount =
            query3.andQualifier(exp3).fetchSingle();

          if (anotherAccount == null) {
            mailAccount.setAccountType(Integer.toString(ACCOUNT_TYPE_DEFAULT));
          } else {
            mailAccount.setAccountType(Integer.toString(ACCOUNT_TYPE_NON));
          }
        } else {
          mailAccount = account;
          if (Integer.toString(ACCOUNT_TYPE_INIT).equals(
            account.getAccountType())) {
            enableUpdate = true;
            mailAccount.setAccountType(Integer.toString(ACCOUNT_TYPE_DEFAULT));
          } else {
            mailAccount.setAccountType(Integer.toString(ACCOUNT_TYPE_NON));
          }
        }
      }

      // ユーザーID
      mailAccount.setUserId(Integer.valueOf(userId));
      mailAccount.setAccountName(accountName);
      mailAccount.setSmtpserverName(smtpServerName);
      mailAccount.setPop3serverName(Pop3ServerName);
      mailAccount.setPop3userName(pop3UserName);
      mailAccount.setPop3password(ALMailUtils
        .getEncryptedMailAccountPasswd(pop3Password.getBytes()));
      mailAccount.setMailUserName(mailUserName);
      mailAccount.setMailAddress(mailAddress);
      mailAccount.setSmtpPort(Integer.toString(smtpPort));
      mailAccount.setPop3Port(Integer.toString(pop3Port));
      mailAccount.setAuthSendFlg((short) authSendFlag);
      mailAccount.setAuthSendUserId(authSendUserId);
      if (authSendUserPasswd != null) {
        mailAccount.setAuthSendUserPasswd(ALMailUtils
          .getEncryptedMailAccountPasswd(authSendUserPasswd.getBytes()));
      }
      mailAccount.setSmtpEncryptionFlg((short) smtpEncryptionFlg);
      mailAccount.setPop3EncryptionFlg((short) pop3EncryptionFlg);
      mailAccount.setAuthReceiveFlg(Short.valueOf((short) authReceiveFlg));
      mailAccount.setDelAtPop3Flg(Integer.valueOf(delAtPop3Flg).toString());
      mailAccount.setDelAtPop3BeforeDaysFlg(Integer.valueOf(
        delAtPop3BeforeDaysFlg).toString());
      mailAccount.setDelAtPop3BeforeDays(Integer.valueOf(delAtPop3BeforeDays));
      mailAccount.setNonReceivedFlg(nonReceivedFlg);
      mailAccount.setUpdateDate(createdDate);
      mailAccount.setSignature(signature);

      if (!enableUpdate) {
        // 新規作成日
        mailAccount.setCreateDate(createdDate);
      }

      // フォルダ「受信トレイ」を作成する
      mailFolder = Database.create(EipTMailFolder.class);
      mailFolder.setEipMMailAccount(mailAccount);
      mailFolder.setFolderName(EipTMailFolder.DEFAULT_FOLDER_NAME);
      mailFolder.setCreateDate(createdDate);
      mailFolder.setUpdateDate(createdDate);
      Database.commit();

      // デフォルトのフォルダID
      mailAccount.setDefaultFolderId(mailFolder.getFolderId());
      Database.commit();

      // イベントログに保存
      ALEventlogFactoryService.getInstance().getEventlogHandler().log(
        mailAccount.getAccountId(),
        ALEventlogConstants.PORTLET_TYPE_WEBMAIL_ACCOUNT,
        mailAccount.getAccountName());
    } catch (Throwable t) {
      Database.rollback();
      logger.error("ALMailUtils.insertMailAccountData", t);
      return false;
    }
    return true;
  }

  public static boolean sendMailDelegateOne(String org_id, int srcUserId,
      ALEipUserAddr userAddr, String pcSubject, String cellularSubject,
      String pcBody, String cellularBody, int destType, List<String> msgList)
      throws Exception {

    List<ALEipUserAddr> rcptUsers = new ArrayList<ALEipUserAddr>();
    rcptUsers.add(userAddr);

    return sendMailDelegate(
      org_id,
      srcUserId,
      rcptUsers,
      pcSubject,
      cellularSubject,
      pcBody,
      cellularBody,
      destType,
      msgList);
  }

  /**
   * 
   * @param org_id
   * @param srcUserId
   * @param destMemberList
   * @param pcSubject
   * @param cellularSubject
   * @param pcBody
   * @param cellularBody
   * @param destType
   * @param msgList
   * @return
   * @throws Exception
   * 
   * @deprecated {@link ALMailService#sendAdminMail}
   */
  @Deprecated
  public static boolean sendMailDelegate(String org_id, int srcUserId,
      List<ALEipUserAddr> destMemberList, String pcSubject,
      String cellularSubject, String pcBody, String cellularBody, int destType,
      List<String> msgList) throws Exception {

    if (destType < VALUE_MSGTYPE_DEST_NONE
      || destType > VALUE_MSGTYPE_DEST_PC_CELLULAR) {
      return false;
    }

    if (destMemberList == null || destMemberList.size() == 0) {
      return false;
    }

    // メールの送信
    EipMMailAccount account = getEipMMailAccountForAdmin();
    int successSendToPc = ALSmtpMailSender.SEND_MSG_SUCCESS;
    int successSendToCell = ALSmtpMailSender.SEND_MSG_SUCCESS;

    if (account == null) {
      // メールアカウントがない場合
      if (destType == VALUE_MSGTYPE_DEST_PC) {
        successSendToPc = ALSmtpMailSender.SEND_MSG_FAIL_NO_ACCOUNT;
      } else if (destType == VALUE_MSGTYPE_DEST_CELLULAR) {
        successSendToCell = ALSmtpMailSender.SEND_MSG_FAIL_NO_ACCOUNT;
      } else {
        successSendToPc = ALSmtpMailSender.SEND_MSG_FAIL_NO_ACCOUNT;
        successSendToCell = ALSmtpMailSender.SEND_MSG_FAIL_NO_ACCOUNT;
      }
    } else {
      List<String> destEmailAddrs = new ArrayList<String>();
      List<String> destCellularEMailAddrs = new ArrayList<String>();

      for (ALEipUserAddr userAddr : destMemberList) {
        if (ALEipUtils.isEnabledUser(userAddr.getUserId())) {
          String emailAddr = userAddr.getPcMailAddr();
          if (emailAddr != null && !emailAddr.equals("")) {
            destEmailAddrs.add(emailAddr);
          }
          String cellularEmailAddr = userAddr.getCellMailAddr();
          if (cellularEmailAddr != null && !cellularEmailAddr.equals("")) {
            destCellularEMailAddrs.add(cellularEmailAddr);
          }
        }
      }

      int destEmailAddrsSize = destEmailAddrs.size();
      int destCellularEMailAddrsSize = destCellularEMailAddrs.size();

      ALMailHandler mailhandler =
        ALMailFactoryService.getInstance().getMailHandler();
      // 送信サーバ情報
      ALMailSenderContext scontext =
        ALMailUtils.getALSmtpMailSenderContext(org_id, account);

      // パソコンへメールを送信
      if ((destType == VALUE_MSGTYPE_DEST_PC || destType == VALUE_MSGTYPE_DEST_PC_CELLULAR)
        && (destEmailAddrsSize > 0)) {
        String[] tos = new String[destEmailAddrsSize];
        tos = destEmailAddrs.toArray(tos);

        // 送信メッセージのコンテキスト
        ALSmtpMailContext mailcontext =
          ALMailUtils.getALSmtpMailContext(
            tos,
            null,
            null,
            account.getMailAddress(),
            ALStringUtil.unsanitizing(account.getMailUserName()),
            ALStringUtil.unsanitizing(pcSubject),
            ALStringUtil.unsanitizing(pcBody),
            null,
            null);

        successSendToPc = mailhandler.send(scontext, mailcontext);
      }

      // 携帯電話へメールを送信
      if ((destType == VALUE_MSGTYPE_DEST_CELLULAR || destType == VALUE_MSGTYPE_DEST_PC_CELLULAR)
        && (destCellularEMailAddrsSize > 0)) {
        String[] tos = new String[destCellularEMailAddrsSize];
        tos = destCellularEMailAddrs.toArray(tos);

        ALSmtpMailContext mailcontext =
          ALMailUtils.getALSmtpMailContext(
            tos,
            null,
            null,
            account.getMailAddress(),
            ALStringUtil.unsanitizing(account.getMailUserName()),
            ALStringUtil.unsanitizing(cellularSubject),
            ALStringUtil.unsanitizing(cellularBody),
            null,
            null);

        successSendToCell = mailhandler.send(scontext, mailcontext);
      }
    }

    if (successSendToPc != ALSmtpMailSender.SEND_MSG_SUCCESS) {
      if (successSendToPc == ALSmtpMailSender.SEND_MSG_OVER_MAIL_MAX_SIZE) {
        msgList.add("メールサイズが送信可能サイズよりも大きいため、パソコンのメールアドレスにメールを送信できませんでした。");
      } else if (successSendToPc == ALSmtpMailSender.SEND_MSG_LOCK) {
        msgList.add("ロックがかかっていて、パソコンのメールアドレスにメールを送信できませんでした。");
      } else if (successSendToPc == ALSmtpMailSender.SEND_MSG_FAIL_POP_BEFORE_SMTP_AUTH) {
        msgList.add("Pop before SMTPの認証に失敗したため、パソコンのメールアドレスにメールを送信できませんでした。");
      } else if (successSendToPc == ALSmtpMailSender.SEND_MSG_FAIL_SMTP_AUTH) {
        msgList.add("SMTP認証の認証に失敗したため、パソコンのメールアドレスにメールを送信できませんでした。");
      } else if (successSendToPc == ALSmtpMailSender.SEND_MSG_FAIL_NO_ACCOUNT) {
        msgList.add("管理者のメールアカウントが設定されていないため、パソコンのメールアドレスにメールを送信できませんでした。");
      } else {
        msgList.add("送信メールサーバに接続できなかったため、パソコンのメールアドレスにメールを送信できませんでした。");
      }
    }

    if (successSendToCell != ALSmtpMailSender.SEND_MSG_SUCCESS) {
      if (successSendToCell == ALSmtpMailSender.SEND_MSG_OVER_MAIL_MAX_SIZE) {
        msgList.add("メールサイズが送信可能サイズよりも大きいため、携帯のメールアドレスにメールを送信できませんでした。");
      } else if (successSendToCell == ALSmtpMailSender.SEND_MSG_LOCK) {
        msgList.add("ロックがかかっていて、携帯のメールアドレスにメールを送信できませんでした。");
      } else if (successSendToCell == ALSmtpMailSender.SEND_MSG_FAIL_POP_BEFORE_SMTP_AUTH) {
        msgList.add("Pop before SMTPの認証に失敗したため、携帯のメールアドレスにメールを送信できませんでした。");
      } else if (successSendToCell == ALSmtpMailSender.SEND_MSG_FAIL_SMTP_AUTH) {
        msgList.add("SMTP認証の認証に失敗したため、携帯のメールアドレスにメールを送信できませんでした。");
      } else if (successSendToCell == ALSmtpMailSender.SEND_MSG_FAIL_NO_ACCOUNT) {
        msgList.add("管理者のメールアカウントが設定されていないため、携帯のメールアドレスにメールを送信できませんでした。");
      } else {
        msgList.add("送信メールサーバに接続できなかったため、携帯のメールアドレスにメールを送信できませんでした。");
      }
    }

    return (successSendToPc == ALSmtpMailSender.SEND_MSG_SUCCESS && successSendToCell == ALSmtpMailSender.SEND_MSG_SUCCESS);
  }

  /**
   * 
   * @param org_id
   * @param srcUserId
   * @param destMemberList
   * @param pcSubject
   * @param cellularSubject
   * @param pcBody
   * @param cellularBody
   * @param destType
   * @param msgList
   * @return
   * @throws Exception
   */
  @Deprecated
  public static boolean sendMailDelegate_note(String org_id, int srcUserId,
      List<ALEipUserAddr> destMemberList, String pcSubject,
      String cellularSubject, String pcBody, String cellularBody, int destType,
      List<String> msgList) throws Exception {

    if (destType < VALUE_MSGTYPE_DEST_NONE
      || destType > VALUE_MSGTYPE_DEST_PC_CELLULAR) {
      return false;
    }

    if (destMemberList == null || destMemberList.size() == 0) {
      return false;
    }

    // メールの送信
    EipMMailAccount account = getEipMMailAccountForAdmin();
    int successSendToPc = ALSmtpMailSender.SEND_MSG_SUCCESS;
    int successSendToCell = ALSmtpMailSender.SEND_MSG_SUCCESS;

    if (account == null) {
      // メールアカウントがない場合
      if (destType == VALUE_MSGTYPE_DEST_PC) {
        successSendToPc = ALSmtpMailSender.SEND_MSG_FAIL_NO_ACCOUNT;
      } else if (destType == VALUE_MSGTYPE_DEST_CELLULAR) {
        successSendToCell = ALSmtpMailSender.SEND_MSG_FAIL_NO_ACCOUNT;
      } else {
        successSendToPc = ALSmtpMailSender.SEND_MSG_FAIL_NO_ACCOUNT;
        successSendToCell = ALSmtpMailSender.SEND_MSG_FAIL_NO_ACCOUNT;
      }
    } else {
      List<String> destEmailAddrs = new ArrayList<String>();
      List<String> destCellularEmailAddrs = new ArrayList<String>();

      for (ALEipUserAddr userAddr : destMemberList) {
        if (ALEipUtils.isEnabledUser(userAddr.getUserId())) {
          String emailAddr = userAddr.getPcMailAddr();
          if (emailAddr != null && !emailAddr.equals("")) {
            destEmailAddrs.add(emailAddr);
          }
          String cellularEmailAddr = userAddr.getCellMailAddr();
          if (cellularEmailAddr != null && !cellularEmailAddr.equals("")) {
            destCellularEmailAddrs.add(cellularEmailAddr);
          }
        }
      }

      int destEmailAddrsSize = destEmailAddrs.size();
      int destCellularEmailAddrsSize = destCellularEmailAddrs.size();

      ALMailHandler mailhandler =
        ALMailFactoryService.getInstance().getMailHandler();
      // 送信サーバ情報
      ALMailSenderContext scontext =
        ALMailUtils.getALSmtpMailSenderContext(org_id, account);

      // パソコンへメールを送信
      if ((destType == VALUE_MSGTYPE_DEST_PC || destType == VALUE_MSGTYPE_DEST_PC_CELLULAR)
        && (destEmailAddrsSize > 0)) {
        String[] tos = new String[destEmailAddrsSize];
        tos = destEmailAddrs.toArray(tos);

        // 送信メッセージのコンテキスト
        ALSmtpMailContext mailcontext =
          ALMailUtils.getALSmtpMailContext(
            tos,
            null,
            null,
            account.getMailAddress(),
            ALStringUtil.unsanitizing(account.getMailUserName()),
            ALStringUtil.unsanitizing(pcSubject),
            ALStringUtil.unsanitizing(pcBody),
            null,
            null);

        successSendToPc = mailhandler.send(scontext, mailcontext);
      }
      // PCへ送信するにチェックはしてあるが, 送信先PCのアドレスが未登録
      if (((destType == VALUE_MSGTYPE_DEST_PC) || (destType == VALUE_MSGTYPE_DEST_PC_CELLULAR))
        && destEmailAddrsSize <= 0) {
        successSendToPc = -1;
      }

      // 携帯電話へメールを送信
      if (((destType == VALUE_MSGTYPE_DEST_CELLULAR) || (destType == VALUE_MSGTYPE_DEST_PC_CELLULAR))
        && (destCellularEmailAddrsSize > 0)) {
        String[] tos = new String[destCellularEmailAddrsSize];
        tos = destCellularEmailAddrs.toArray(tos);

        ALSmtpMailContext mailcontext =
          ALMailUtils.getALSmtpMailContext(
            tos,
            null,
            null,
            account.getMailAddress(),
            ALStringUtil.unsanitizing(account.getMailUserName()),
            ALStringUtil.unsanitizing(cellularSubject),
            ALStringUtil.unsanitizing(cellularBody),
            null,
            null);

        successSendToCell = mailhandler.send(scontext, mailcontext);

      }
      // 携帯へ送信するにチェックはしてあるが, 送信先携帯のアドレスが未登録
      if (((destType == VALUE_MSGTYPE_DEST_CELLULAR) || (destType == VALUE_MSGTYPE_DEST_PC_CELLULAR))
        && destCellularEmailAddrsSize <= 0) {
        successSendToCell = -1;
      }
    }

    if (successSendToPc != ALSmtpMailSender.SEND_MSG_SUCCESS) {
      if (successSendToPc == ALSmtpMailSender.SEND_MSG_OVER_MAIL_MAX_SIZE) {
        msgList.add("メールサイズが送信可能サイズよりも大きいため、パソコンのメールアドレスにメールを送信できませんでした。");
      } else if (successSendToPc == ALSmtpMailSender.SEND_MSG_LOCK) {
        msgList.add("ロックがかかっていて、パソコンのメールアドレスにメールを送信できませんでした。");
      } else if (successSendToPc == ALSmtpMailSender.SEND_MSG_FAIL_POP_BEFORE_SMTP_AUTH) {
        msgList.add("Pop before SMTPの認証に失敗したため、パソコンのメールアドレスにメールを送信できませんでした。");
      } else if (successSendToPc == ALSmtpMailSender.SEND_MSG_FAIL_SMTP_AUTH) {
        msgList.add("SMTP認証の認証に失敗したため、パソコンのメールアドレスにメールを送信できませんでした。");
      } else if (successSendToPc == ALSmtpMailSender.SEND_MSG_FAIL_NO_ACCOUNT) {
        msgList.add("管理者のメールアカウントが設定されていないため、パソコンのメールアドレスにメールを送信できませんでした。");
      } else if (successSendToPc == -1) {
        msgList.add("送信先のメールアカウントが設定されていないため、パソコンのメールアドレスにメールを送信できませんでした。");
      } else {
        msgList.add("送信メールサーバに接続できなかったため、パソコンのメールアドレスにメールを送信できませんでした。");
      }
    }

    if (successSendToCell != ALSmtpMailSender.SEND_MSG_SUCCESS) {
      if (successSendToCell == ALSmtpMailSender.SEND_MSG_OVER_MAIL_MAX_SIZE) {
        msgList.add("メールサイズが送信可能サイズよりも大きいため、携帯のメールアドレスにメールを送信できませんでした。");
      } else if (successSendToCell == ALSmtpMailSender.SEND_MSG_LOCK) {
        msgList.add("ロックがかかっていて、携帯のメールアドレスにメールを送信できませんでした。");
      } else if (successSendToCell == ALSmtpMailSender.SEND_MSG_FAIL_POP_BEFORE_SMTP_AUTH) {
        msgList.add("Pop before SMTPの認証に失敗したため、携帯のメールアドレスにメールを送信できませんでした。");
      } else if (successSendToCell == ALSmtpMailSender.SEND_MSG_FAIL_SMTP_AUTH) {
        msgList.add("SMTP認証の認証に失敗したため、携帯のメールアドレスにメールを送信できませんでした。");
      } else if (successSendToCell == ALSmtpMailSender.SEND_MSG_FAIL_NO_ACCOUNT) {
        msgList.add("管理者のメールアカウントが設定されていないため、携帯のメールアドレスにメールを送信できませんでした。");
      } else if (successSendToCell == -1) {
        msgList.add("送信先の携帯メールアドレスが設定されていないため、携帯のメールアドレスにメールを送信できませんでした。");
      } else {
        msgList.add("送信メールサーバに接続できなかったため、携帯のメールアドレスにメールを送信できませんでした。");
      }
    }

    return (successSendToPc == ALSmtpMailSender.SEND_MSG_SUCCESS && successSendToCell == ALSmtpMailSender.SEND_MSG_SUCCESS);
  }

  public static ALEipUserAddr getALEipUserAddrByUserId(int userId) {
    try {
      ALEipUserAddr userAddress = new ALEipUserAddr();
      TurbineUser user = ALEipUtils.getTurbineUser(userId);

      userAddress.setUserId(user.getUserId());
      userAddress.setPcMailAddr(user.getEmail());
      userAddress.setCellMailAddr(user.getCellularMail());

      return userAddress;
    } catch (ALDBErrorException e) {
      logger.error("ALMailUtils.getALEipUserAddrByUserId", e);
      return null;
    }

  }

  /**
   * ALEipUserのリストをもとに、ALEipUserAddrのリストを取得する。
   * 
   * @param memberList
   * @param loginUserId
   * @param includeLoginUser
   * @return
   */
  public static List<ALEipUserAddr> getALEipUserAddrs(
      List<ALEipUser> memberList, int loginUserId, boolean includeLoginUser) {
    List<ALEipUserAddr> resList = new ArrayList<ALEipUserAddr>();

    ALEipUserAddr useraddr = null;
    int membersize = memberList.size();
    for (int i = 0; i < membersize; i++) {
      ALEipUser user = memberList.get(i);
      if (!includeLoginUser && (user.getUserId().getValue() == loginUserId)) {
        // ログインユーザをメール送信先から外す
        continue;
      }
      try {
        ALBaseUser baseuser =
          (ALBaseUser) JetspeedSecurity.getUser(user.getName().getValue());
        useraddr = new ALEipUserAddr();
        useraddr.setUserId(Integer.valueOf(baseuser.getUserId()));
        useraddr.setPcMailAddr(baseuser.getEmail());
        useraddr.setCellMailAddr(baseuser.getCellularMail());
        resList.add(useraddr);
      } catch (Exception ex) {
        logger.error("ALMailUtils.getALEipUserAddrs", ex);
      }
    }

    return resList;
  }

  /**
   * メール通知設定表から送信先の設定を取得する。
   * 
   * @param keyMsgtype
   * @return
   */
  public static int getSendDestType(int keyMsgtype) {
    try {
      SelectQuery<EipMMailNotifyConf> query =
        Database.query(EipMMailNotifyConf.class);

      Expression exp1 =
        ExpressionFactory.matchExp(EipMMailNotifyConf.USER_ID_PROPERTY, Integer
          .valueOf(1));
      Expression exp2 =
        ExpressionFactory.matchExp(
          EipMMailNotifyConf.NOTIFY_TYPE_PROPERTY,
          Integer.valueOf(keyMsgtype));
      EipMMailNotifyConf mail_notify_conf =
        query.andQualifier(exp1).andQualifier(exp2).fetchSingle();

      if (mail_notify_conf == null) {
        logger.debug("[ALMailUtils] Not found Notify...");
        return VALUE_MSGTYPE_DEST_NONE;
      }
      return mail_notify_conf.getNotifyFlg().intValue();
    } catch (Exception ex) {
      logger.error("ALMailUtils.getSendDestType", ex);
      return VALUE_MSGTYPE_DEST_NONE;
    }
  }

  /**
   * メール通知設定表から送信先の設定を取得する。
   * 
   * @param keyMsgtype
   * @return
   */
  public static boolean setSendDestType(int keyMsgtype, int valueMsgtype) {
    try {
      if (valueMsgtype < VALUE_MSGTYPE_DEST_NONE
        || valueMsgtype > VALUE_MSGTYPE_DEST_PC_CELLULAR) {
        return false;
      }

      SelectQuery<EipMMailNotifyConf> query =
        Database.query(EipMMailNotifyConf.class);

      Expression exp1 =
        ExpressionFactory.matchExp(
          EipMMailNotifyConf.NOTIFY_TYPE_PROPERTY,
          Integer.valueOf(keyMsgtype));
      EipMMailNotifyConf mail_notify_conf =
        query.andQualifier(exp1).fetchSingle();

      if (mail_notify_conf == null) {
        logger.debug("[ALMailUtils] Not found Notify...");
        return false;
      }
      mail_notify_conf.setNotifyFlg(valueMsgtype);
      Database.commit();
    } catch (Throwable t) {
      Database.rollback();
      logger.error("ALMailUtils.setSendDestType", t);
      return false;
    }
    return true;
  }

  public static String getGlobalurl() {
    EipMCompany record = ALEipUtils.getEipMCompany("1");
    return ALServletUtils.getAccessUrl(record.getIpaddress(), record
      .getPort()
      .intValue(), true);
  }

  public static String getLocalurl() {
    EipMCompany record = ALEipUtils.getEipMCompany("1");
    String localurl = "";

    try {
      String ipaddress = record.getIpaddressInternal();
      if (null == ipaddress || "".equals(ipaddress)) {
        Enumeration<NetworkInterface> enuIfs =
          NetworkInterface.getNetworkInterfaces();
        if (null != enuIfs) {
          while (enuIfs.hasMoreElements()) {
            NetworkInterface ni = enuIfs.nextElement();
            Enumeration<InetAddress> enuAddrs = ni.getInetAddresses();
            while (enuAddrs.hasMoreElements()) {
              InetAddress in4 = enuAddrs.nextElement();
              if (!in4.isLoopbackAddress()) {
                ipaddress = in4.getHostAddress();
              }
            }
          }
        }
      }
      Integer port_internal = record.getPortInternal();
      if (null == port_internal) {
        port_internal = 80;
      }
      localurl = ALServletUtils.getAccessUrl(ipaddress, port_internal, false);
    } catch (SocketException e) {
      logger.error("ALMailUtils.getLocalurl", e);
    }
    return localurl;
  }

  /**
   * 指定されたIDのメール通知設定表を取得します。
   * 
   * @param category_id
   * @return
   */
  public static EipMMailNotifyConf getEipMMailNotifyConf(int conf_id) {
    try {
      EipMMailNotifyConf result =
        Database.get(EipMMailNotifyConf.class, conf_id);

      if (result == null) {
        logger.debug("[ALMailUtils] Not found ID...");
        return null;
      }
      return result;
    } catch (Exception ex) {
      logger.error("ALMailUtils.getEipMMailNotifyConf", ex);
      return null;
    }
  }

  public static boolean setNotifyTime(int hour, int minute) {
    StringBuffer sb = new StringBuffer();
    if (hour < 10) {
      sb.append("0");
    }
    sb.append(Integer.toString(hour)).append(":");
    if (minute < 10) {
      sb.append("0");
    }
    sb.append(Integer.toString(minute)).append(":00");
    Time time = Time.valueOf(sb.toString());

    try {
      EipMMailNotifyConf conf = getEipMMailNotifyConf(1);
      if (conf == null) {
        return false;
      }

      conf.setNotifyTime(time);
      Database.commit();
      return true;
    } catch (Throwable t) {
      Database.rollback();
      logger.error("ALMailUtils.setNotifyTime", t);
      return false;
    }
  }

  public static String getNotifyTime() {
    try {
      EipMMailNotifyConf conf = getEipMMailNotifyConf(1);
      if (conf == null) {
        return null;
      }

      Date date = conf.getNotifyTime();
      Calendar cal = Calendar.getInstance();
      cal.setTime(date);

      StringBuffer sb = new StringBuffer();
      int hour = cal.get(Calendar.HOUR_OF_DAY);
      int minute = cal.get(Calendar.MINUTE);
      if (hour < 10) {
        sb.append("0");
      }
      sb.append(hour).append(":");
      if (minute < 10) {
        sb.append("0");
      }
      sb.append(minute);

      return sb.toString();
    } catch (Exception e) {
      logger.error("ALMailUtils.getNotifyTime", e);
      return null;
    }
  }

  /**
   * フォルダオブジェクトモデルを取得します。 <BR>
   * 
   * @param rundata
   * @param context
   * @return
   */
  public static List<EipTMailFolder> getEipTMailFolderAll(
      EipMMailAccount account) {
    try {
      if (account == null) {
        // アカウントが空の場合
        logger.debug("[WebMail Folder] Empty Account...");
        return null;
      }

      SelectQuery<EipTMailFolder> query = Database.query(EipTMailFolder.class);

      Expression exp =
        ExpressionFactory.matchDbExp(
          EipTMailFolder.EIP_MMAIL_ACCOUNT_PROPERTY,
          account);
      List<EipTMailFolder> folder_list =
        query.andQualifier(exp).orderAscending(
          EipTMailFolder.FOLDER_NAME_PROPERTY).fetchList();

      if (folder_list == null || folder_list.size() == 0) {
        logger.debug("[WebMail Folder] Not found ID...");
        return null;
      }

      // 受信トレイを先頭に配置する
      List<EipTMailFolder> res = new ArrayList<EipTMailFolder>();
      for (EipTMailFolder folder : folder_list) {
        if (folder.getFolderName().equals(EipTMailFolder.DEFAULT_FOLDER_NAME)) {
          EipTMailFolder inbox = folder;
          folder_list.remove(folder);
          res.add(inbox);
          res.addAll(folder_list);
          break;
        }
      }
      return res.size() == 0 ? folder_list : res;
    } catch (Exception ex) {
      logger.error("ALMailUtils.getEipTMailFolderAll", ex);
      return null;
    }
  }

  /**
   * 指定されたフォルダに入っているメールを全て取得します。 <BR>
   * 
   * @param rundata
   * @param context
   * @return
   */
  public static List<EipTMail> getEipTMails(EipTMailFolder folder) {
    try {
      if (folder == null) {
        logger.debug("[WebMail Folder] Empty Folder...");
        return null;
      }
      SelectQuery<EipTMail> query = Database.query(EipTMail.class);

      Expression exp =
        ExpressionFactory.matchExp(EipTMail.FOLDER_ID_PROPERTY, folder
          .getFolderId());
      List<EipTMail> mail_list = query.andQualifier(exp).fetchList();

      if (mail_list == null || mail_list.size() == 0) {
        logger.debug("[WebMail Folder] No Mail in the Folder...");
        return null;
      }
      return mail_list;
    } catch (Exception ex) {
      logger.error("ALMailUtils.getEipTMails", ex);
      return null;
    }
  }

  /**
   * フィルタオブジェクトモデルを取得します。（アカウントで検索） <BR>
   * 
   * @param rundata
   * @param context
   * @return
   */
  public static List<EipTMailFilter> getEipTMailFilters(
      EipMMailAccount mailAccount) {
    try {
      SelectQuery<EipTMailFilter> query = Database.query(EipTMailFilter.class);

      Expression exp =
        ExpressionFactory.matchExp(
          EipTMailFilter.EIP_MMAIL_ACCOUNT_PROPERTY,
          mailAccount);
      List<EipTMailFilter> filter_list =
        query.andQualifier(exp).orderAscending(
          EipTMailFilter.SORT_ORDER_PROPERTY).fetchList();

      if (filter_list == null || filter_list.size() == 0) {
        logger.debug("[WebMail Filter] Not found ID...");
        return null;
      }
      return filter_list;
    } catch (Exception ex) {
      logger.error("ALMailUtils.getEipTMailFilters", ex);
      return null;
    }
  }

  /**
   * メールがフィルタの条件(件名、送信元メールアドレス、送信先メールアドレス)に合致するかどうか調べます。
   * 
   * @param mailFilter
   * @param subject
   * @param from
   * @param receivers
   * @return boolean
   */
  public static boolean isMatchFilter(EipTMailFilter mailFilter,
      String subject, String from, Address[] receivers) {
    String filterType = mailFilter.getFilterType();
    String filterString = mailFilter.getFilterString();
    // int dstFolderId = mailFilter.getEipTMailFolder().getFolderId();

    if (FILTER_TYPE_DOMAIN.equals(filterType)) {
      // ドメインを含む
      try {
        String[] domainArray = from.split("@");
        String domain = domainArray[domainArray.length - 1];
        return domain.toLowerCase().contains(filterString.toLowerCase());
      } catch (Exception e) {
        return false;
      }
    } else if (FILTER_TYPE_MAILADDRESS.equals(filterType)) {
      // メールアドレスを含む
      String[] mailAddrArray = from.split("<");
      String mailAddr = mailAddrArray[mailAddrArray.length - 1];
      return mailAddr.toLowerCase().contains(filterString.toLowerCase());

    } else if (FILTER_TYPE_SUBJECT.equals(filterType)) {
      // 件名を含む
      return decodeSubject(subject).toLowerCase().contains(
        filterString.toLowerCase());
    } else if (FILTER_TYPE_TO.equals(filterType)) {
      for (Address address : receivers) {
        InternetAddress iadress = (InternetAddress) address;
        // String personal = iadress.getPersonal();
        String email = iadress.getAddress();
        if (email.toLowerCase().contains(filterString.toLowerCase())) {
          return true;
        }
      }
      return false;
    }

    return false;
  }

  /**
   * フィルタタイプの一覧を取得します。 <BR>
   * 
   * @param rundata
   * @param context
   * @return
   */
  public static Map<String, String> getMailFilterTypeMap() {
    Map<String, String> typeMap = new TreeMap<String, String>();
    typeMap.put(FILTER_TYPE_MAILADDRESS, "送信元（From）");
    typeMap.put(FILTER_TYPE_TO, "送信先（To）");
    // ドメインは送信元に包括されるのでコメントアウト
    // typeMap.put(FILTER_TYPE_DOMAIN, "ドメイン");
    typeMap.put(FILTER_TYPE_SUBJECT, "件名");

    return typeMap;
  }

  /**
   * 件名のデコードを行ないます。
   * 
   * @param subject
   * @return
   */
  public static String decodeSubject(String subject) {
    try {
      subject = MimeUtility.decodeText(MimeUtility.unfold(subject));
      return UnicodeCorrecter.correctToCP932(MailUtility.decodeText(subject));
    } catch (UnsupportedEncodingException e) {
      return MailUtility.decodeText(subject);
    }
  }

  /** 送受信可能なメール容量．Base64 処理後のサイズ */
  public static int getMaxMailSize() {
    return (int) (TurbineUpload.getSizeMax() * 1.37);
  }

  public static String getRawFrom(MimeMessage msg) {
    String rawFrom = null;
    try {
      rawFrom = msg.getHeader("From", ",");
      if (null == rawFrom) {
        rawFrom = msg.getHeader("Sender", ",");
      }
    } catch (MessagingException ignore) {
    }
    return null != rawFrom ? rawFrom : "";
  }

  public static String getRawReplyTo(MimeMessage msg) {
    String rawReplyto = null;
    try {
      rawReplyto = msg.getHeader("Reply-To", ",");

      if (rawReplyto == null) {
        rawReplyto = getRawFrom(msg);
      }
    } catch (MessagingException ignore) {
    }
    return null != rawReplyto ? rawReplyto : "";
  }

  public static String getFromInetAddressForBroken(MimeMessage msg) {
    String rawFrom = getRawFrom(msg);
    String inetAddress = null;
    try {
      inetAddress = MimeUtility.decodeText(MimeUtility.unfold(rawFrom));
    } catch (UnsupportedEncodingException ignore) {
    }
    return null != inetAddress ? inetAddress : "";
  }

  public static String getReplytoInetAddressForBroken(MimeMessage msg) {
    String rawReplyto = getRawReplyTo(msg);
    String inetAddress = null;
    try {
      inetAddress = MimeUtility.decodeText(MimeUtility.unfold(rawReplyto));
    } catch (UnsupportedEncodingException ignore) {
    }
    return null != inetAddress ? inetAddress : "";
  }

  public static String extractAddress(String rawAddress) {
    String regex =
      "[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})";
    Pattern mailPattern = Pattern.compile(regex);
    Matcher matcher = mailPattern.matcher(rawAddress);
    if (matcher.find()) {
      String result = matcher.group(0);
      return null != result ? result : "";
    } else {
      return "";
    }
  }

  public static String getFromDelegate(MimeMessage msg)
      throws MessagingException {
    String from = null;
    try {
      from = ALMailUtils.getAddressString(msg.getFrom());
    } catch (AddressException e) {
      logger.info("ALMailUtils.getFromDelegate", e);
      from = ALMailUtils.getFromInetAddressForBroken(msg);
    }
    return from;
  }

  public static String getFromDelegateExtract(MimeMessage msg)
      throws MessagingException {
    String from = null;
    try {
      from = ALMailUtils.getAddressString(msg.getFrom());
    } catch (AddressException e) {
      logger.info("ALMailUtils.getFromDelegateExtract", e);
      from = ALMailUtils.getFromInetAddressForBroken(msg);
      from = ALMailUtils.extractAddress(from);
    }
    return from;
  }

  public static Address[] getFromDelegateExtractForAddress(MimeMessage msg)
      throws MessagingException {
    Address[] addresses = new InternetAddress[1];
    try {
      addresses = msg.getFrom();
    } catch (AddressException e) {
      logger.info("ALMailUtils.getFromDelegateExtractForAddress", e);
      String from = ALMailUtils.getFromInetAddressForBroken(msg);
      from = ALMailUtils.extractAddress(from);
      Address address = new InternetAddress(from, false);
      addresses[0] = address;
    }
    return addresses;
  }

  public static Address getReplyToDelegateExtract(MimeMessage msg)
      throws MessagingException {
    Address replyTo = null;
    try {
      replyTo = msg.getReplyTo()[0];
    } catch (AddressException e) {
      logger.info("ALMailUtils.getReplyToDelegateExtract", e);
      String _replyTo = getReplytoInetAddressForBroken(msg);
      _replyTo = ALMailUtils.extractAddress(_replyTo);
      replyTo = new InternetAddress(_replyTo, false);
    }
    return replyTo;
  }
}
