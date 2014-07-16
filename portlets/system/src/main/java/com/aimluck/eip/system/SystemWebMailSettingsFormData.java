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

package com.aimluck.eip.system;

import java.util.List;

import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALNumberField;
import com.aimluck.eip.common.ALAbstractFormData;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.mail.util.ALMailUtils;

/**
 * 管理者メール通知設定のフォームデータを管理するためのクラスです。 <br />
 */

public class SystemWebMailSettingsFormData extends ALAbstractFormData {

  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(SystemWebMailSettingsFormData.class.getName());

  private static final int FLG_NOTIFY_NONCHECKED = 0;

  private static final int FLG_NOTIFY_CHECKED = 1;

  /** パソコンへのメール送信フラグ(スケジュール) */
  private ALNumberField pc_flg_schedule;

  /** 携帯電話へのメール送信フラグ(スケジュール) */
  private ALNumberField cell_flg_schedule;

  /** パソコンへのメール送信フラグ(伝言メモ) */
  private ALNumberField pc_flg_note;

  /** 携帯電話へのメール送信フラグ(伝言メモ) */
  private ALNumberField cell_flg_note;

  /** パソコンへのメール送信フラグ(ブログ) */
  private ALNumberField pc_flg_blog;

  /** 携帯電話へのメール送信フラグ(ブログ) */
  private ALNumberField cell_flg_blog;

  /** パソコンへのメール送信フラグ(ワークフロー) */
  private ALNumberField pc_flg_workflow;

  /** 携帯電話へのメール送信フラグ(ワークフロー) */
  private ALNumberField cell_flg_workflow;

  /** パソコンへのメール送信フラグ(掲示板返信) */
  private ALNumberField pc_flg_msgboard;

  /** 携帯電話へのメール送信フラグ(掲示板返信) */
  private ALNumberField cell_flg_msgboard;

  /** パソコンへのメール送信フラグ(報告書) */
  private ALNumberField pc_flg_report;

  /** 携帯電話へのメール送信フラグ(報告書) */
  private ALNumberField cell_flg_report;

  /** パソコンへのメール送信フラグ(プロジェクト管理) */
  private ALNumberField pc_flg_project;

  /** 携帯電話へのメール送信フラグ(プロジェクト管理) */
  private ALNumberField cell_flg_project;

  /** 通知時間 */
  private ALNumberField notify_time_hour;

  private ALNumberField notify_time_minute;

  /** メール送信時のメッセージ種別(スケジュール) */
  private int msg_type_schedule = FLG_NOTIFY_NONCHECKED;

  /** メール送信時のメッセージ種別(伝言メモ) */
  private int msg_type_note = FLG_NOTIFY_NONCHECKED;

  /** メール送信時のメッセージ種別(ブログ) */
  private int msg_type_blog = FLG_NOTIFY_NONCHECKED;

  /** メール送信時のメッセージ種別(掲示板返信) */
  private int msg_type_workflow = FLG_NOTIFY_NONCHECKED;

  /** メール送信時のメッセージ種別(掲示板返信) */
  private int msg_type_msgboard = FLG_NOTIFY_NONCHECKED;

  /** メール送信時のメッセージ種別(報告書) */
  private int msg_type_report = FLG_NOTIFY_NONCHECKED;

  /** メール送信時のメッセージ種別(プロジェクト管理) */
  private int msg_type_project = FLG_NOTIFY_NONCHECKED;

  @Override
  public void initField() {
    pc_flg_schedule = new ALNumberField();
    pc_flg_schedule.setValue(ALMailUtils.VALUE_MSGTYPE_DEST_NONE);

    cell_flg_schedule = new ALNumberField();
    cell_flg_schedule.setValue(ALMailUtils.VALUE_MSGTYPE_DEST_NONE);

    pc_flg_note = new ALNumberField();
    pc_flg_note.setValue(ALMailUtils.VALUE_MSGTYPE_DEST_NONE);

    cell_flg_note = new ALNumberField();
    cell_flg_note.setValue(ALMailUtils.VALUE_MSGTYPE_DEST_NONE);

    pc_flg_blog = new ALNumberField();
    pc_flg_blog.setValue(ALMailUtils.VALUE_MSGTYPE_DEST_NONE);

    cell_flg_blog = new ALNumberField();
    cell_flg_blog.setValue(ALMailUtils.VALUE_MSGTYPE_DEST_NONE);

    pc_flg_workflow = new ALNumberField();
    pc_flg_workflow.setValue(ALMailUtils.VALUE_MSGTYPE_DEST_NONE);

    cell_flg_workflow = new ALNumberField();
    cell_flg_workflow.setValue(ALMailUtils.VALUE_MSGTYPE_DEST_NONE);

    pc_flg_msgboard = new ALNumberField();
    pc_flg_msgboard.setValue(ALMailUtils.VALUE_MSGTYPE_DEST_NONE);

    cell_flg_msgboard = new ALNumberField();
    cell_flg_msgboard.setValue(ALMailUtils.VALUE_MSGTYPE_DEST_NONE);

    pc_flg_report = new ALNumberField();
    pc_flg_report.setValue(ALMailUtils.VALUE_MSGTYPE_DEST_NONE);

    cell_flg_report = new ALNumberField();
    cell_flg_report.setValue(ALMailUtils.VALUE_MSGTYPE_DEST_NONE);

    pc_flg_project = new ALNumberField();
    pc_flg_project.setValue(ALMailUtils.VALUE_MSGTYPE_DEST_NONE);

    cell_flg_project = new ALNumberField();
    cell_flg_project.setValue(ALMailUtils.VALUE_MSGTYPE_DEST_NONE);

    notify_time_hour = new ALNumberField();
    notify_time_hour.setValue(0);

    notify_time_minute = new ALNumberField();
    notify_time_minute.setValue(0);
  }

  @Override
  protected void setValidator() throws ALPageNotFoundException,
      ALDBErrorException {
  }

  @Override
  protected boolean validate(List<String> msgList)
      throws ALPageNotFoundException, ALDBErrorException {
    return true;
  }

  @Override
  protected boolean loadFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {

    setNotifyFlg(pc_flg_blog, cell_flg_blog, ALMailUtils
      .getSendDestType(ALMailUtils.KEY_MSGTYPE_BLOG));
    setNotifyFlg(pc_flg_note, cell_flg_note, ALMailUtils
      .getSendDestType(ALMailUtils.KEY_MSGTYPE_NOTE));
    setNotifyFlg(pc_flg_schedule, cell_flg_schedule, ALMailUtils
      .getSendDestType(ALMailUtils.KEY_MSGTYPE_SCHEDULE));
    setNotifyFlg(pc_flg_workflow, cell_flg_workflow, ALMailUtils
      .getSendDestType(ALMailUtils.KEY_MSGTYPE_WORKFLOW));
    setNotifyFlg(pc_flg_msgboard, cell_flg_msgboard, ALMailUtils
      .getSendDestType(ALMailUtils.KEY_MSGTYPE_MSGBOARD));
    setNotifyFlg(pc_flg_report, cell_flg_report, ALMailUtils
      .getSendDestType(ALMailUtils.KEY_MSGTYPE_REPORT));
    setNotifyFlg(pc_flg_project, cell_flg_project, ALMailUtils
      .getSendDestType(ALMailUtils.KEY_MSGTYPE_PROJECT));

    String timestr = ALMailUtils.getNotifyTime();
    notify_time_hour.setValue(timestr.charAt(0) == '0' ? timestr
      .substring(1, 2) : timestr.substring(0, 2));
    notify_time_minute.setValue(timestr.charAt(3) == '0' ? timestr.substring(
      4,
      5) : timestr.substring(3, 5));

    return true;
  }

  @Override
  protected boolean setFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
    boolean res = super.setFormData(rundata, context, msgList);
    if (res) {
      if (pc_flg_blog.getValue() == FLG_NOTIFY_CHECKED) {
        msg_type_blog += ALMailUtils.VALUE_MSGTYPE_DEST_PC;
      }
      if (cell_flg_blog.getValue() == FLG_NOTIFY_CHECKED) {
        msg_type_blog += ALMailUtils.VALUE_MSGTYPE_DEST_CELLULAR;
      }

      if (pc_flg_note.getValue() == FLG_NOTIFY_CHECKED) {
        msg_type_note += ALMailUtils.VALUE_MSGTYPE_DEST_PC;
      }
      if (cell_flg_note.getValue() == FLG_NOTIFY_CHECKED) {
        msg_type_note += ALMailUtils.VALUE_MSGTYPE_DEST_CELLULAR;
      }

      if (pc_flg_schedule.getValue() == FLG_NOTIFY_CHECKED) {
        msg_type_schedule += ALMailUtils.VALUE_MSGTYPE_DEST_PC;
      }
      if (cell_flg_schedule.getValue() == FLG_NOTIFY_CHECKED) {
        msg_type_schedule += ALMailUtils.VALUE_MSGTYPE_DEST_CELLULAR;
      }

      if (pc_flg_workflow.getValue() == FLG_NOTIFY_CHECKED) {
        msg_type_workflow += ALMailUtils.VALUE_MSGTYPE_DEST_PC;
      }
      if (cell_flg_workflow.getValue() == FLG_NOTIFY_CHECKED) {
        msg_type_workflow += ALMailUtils.VALUE_MSGTYPE_DEST_CELLULAR;
      }

      if (pc_flg_msgboard.getValue() == FLG_NOTIFY_CHECKED) {
        msg_type_msgboard += ALMailUtils.VALUE_MSGTYPE_DEST_PC;
      }
      if (cell_flg_msgboard.getValue() == FLG_NOTIFY_CHECKED) {
        msg_type_msgboard += ALMailUtils.VALUE_MSGTYPE_DEST_CELLULAR;
      }

      if (pc_flg_report.getValue() == FLG_NOTIFY_CHECKED) {
        msg_type_report += ALMailUtils.VALUE_MSGTYPE_DEST_PC;
      }
      if (cell_flg_report.getValue() == FLG_NOTIFY_CHECKED) {
        msg_type_report += ALMailUtils.VALUE_MSGTYPE_DEST_CELLULAR;
      }

      if (pc_flg_project.getValue() == FLG_NOTIFY_CHECKED) {
        msg_type_project += ALMailUtils.VALUE_MSGTYPE_DEST_PC;
      }
      if (cell_flg_project.getValue() == FLG_NOTIFY_CHECKED) {
        msg_type_project += ALMailUtils.VALUE_MSGTYPE_DEST_CELLULAR;
      }
    }

    return res;
  }

  @Override
  protected boolean insertFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
    return false;
  }

  @Override
  protected boolean updateFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {

    try {
      ALMailUtils.setSendDestType(ALMailUtils.KEY_MSGTYPE_BLOG, msg_type_blog);
      ALMailUtils.setSendDestType(ALMailUtils.KEY_MSGTYPE_NOTE, msg_type_note);
      ALMailUtils.setSendDestType(
        ALMailUtils.KEY_MSGTYPE_SCHEDULE,
        msg_type_schedule);
      ALMailUtils.setSendDestType(
        ALMailUtils.KEY_MSGTYPE_WORKFLOW,
        msg_type_workflow);
      ALMailUtils.setSendDestType(
        ALMailUtils.KEY_MSGTYPE_MSGBOARD,
        msg_type_msgboard);
      ALMailUtils.setSendDestType(
        ALMailUtils.KEY_MSGTYPE_REPORT,
        msg_type_report);
      ALMailUtils.setSendDestType(
        ALMailUtils.KEY_MSGTYPE_PROJECT,
        msg_type_project);
      ALMailUtils.setNotifyTime(
        (int) notify_time_hour.getValue(),
        (int) notify_time_minute.getValue());
    } catch (Exception e) {
      logger.error("system", e);
    }
    return true;
  }

  @Override
  protected boolean deleteFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
    return false;
  }

  private void setNotifyFlg(ALNumberField pc_flg, ALNumberField cell_flg,
      int msgType) {
    if (msgType == ALMailUtils.VALUE_MSGTYPE_DEST_PC) {
      pc_flg.setValue(FLG_NOTIFY_CHECKED);
    } else if (msgType == ALMailUtils.VALUE_MSGTYPE_DEST_CELLULAR) {
      cell_flg.setValue(FLG_NOTIFY_CHECKED);
    } else if (msgType == ALMailUtils.VALUE_MSGTYPE_DEST_PC_CELLULAR) {
      pc_flg.setValue(FLG_NOTIFY_CHECKED);
      cell_flg.setValue(FLG_NOTIFY_CHECKED);
    }
  }

  /**
   * メール送信時のメッセージ種別(スケジュール)
   * 
   * @return
   */
  public ALNumberField getPcFlgSchedule() {
    return pc_flg_schedule;
  }

  /**
   * メール送信時のメッセージ種別(スケジュール)
   * 
   * @return
   */
  public ALNumberField getCellFlgSchedule() {
    return cell_flg_schedule;
  }

  /**
   * メール送信時のメッセージ種別(伝言メモ)
   * 
   * @return
   */
  public ALNumberField getPcFlgNote() {
    return pc_flg_note;
  }

  /**
   * メール送信時のメッセージ種別(伝言メモ)
   * 
   * @return
   */
  public ALNumberField getCellFlgNote() {
    return cell_flg_note;
  }

  /**
   * メール送信時のメッセージ種別(ブログ)
   * 
   * @return
   */
  public ALNumberField getPcFlgBlog() {
    return pc_flg_blog;
  }

  /**
   * メール送信時のメッセージ種別(ブログ)
   * 
   * @return
   */
  public ALNumberField getCellFlgBlog() {
    return cell_flg_blog;
  }

  /**
   * メール送信時のメッセージ種別(ワークフロー)
   * 
   * @return
   */
  public ALNumberField getPcFlgWorkflow() {
    return pc_flg_workflow;
  }

  /**
   * メール送信時のメッセージ種別(ワークフロー)
   * 
   * @return
   */
  public ALNumberField getCellFlgWorkflow() {
    return cell_flg_workflow;
  }

  /**
   * メール送信時のメッセージ種別(掲示板返信)
   * 
   * @return
   */
  public ALNumberField getPcFlgMsgboard() {
    return pc_flg_msgboard;
  }

  /**
   * メール送信時のメッセージ種別(掲示板返信)
   * 
   * @return
   */
  public ALNumberField getCellFlgMsgboard() {
    return cell_flg_msgboard;
  }

  /**
   * メール送信時のメッセージ種別(報告書)
   * 
   * @return
   */
  public ALNumberField getPcFlgReport() {
    return pc_flg_report;
  }

  /**
   * メール送信時のメッセージ種別(報告書)
   * 
   * @return
   */
  public ALNumberField getCellFlgReport() {
    return cell_flg_report;
  }

  /**
   * メール送信時のメッセージ種別(プロジェクト管理)
   * 
   * @return
   */
  public ALNumberField getPcFlgProject() {
    return pc_flg_project;
  }

  /**
   * メール送信時のメッセージ種別(プロジェクト管理)
   * 
   * @return
   */
  public ALNumberField getCellFlgProject() {
    return cell_flg_project;
  }

  /**
   * メール通知時間
   * 
   * @return
   */
  public int getNotifyTimeHour() {
    return (int) notify_time_hour.getValue();
  }

  public int getNotifyTimeMinute() {
    return (int) notify_time_minute.getValue();
  }

}
