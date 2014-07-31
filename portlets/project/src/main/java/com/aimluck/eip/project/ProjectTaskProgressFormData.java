/*
 * Aipo is a groupware program developed by Aimluck,Inc.
 * Copyright (C) 2004-2013 Aimluck,Inc.
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
 *
 * Project Management Portlet was developed by Advance,Inc.
 * http://www.a-dvance.co.jp/
 */

package com.aimluck.eip.project;

import static com.aimluck.eip.util.ALLocalizationUtils.*;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.eip.cayenne.om.portlet.EipTProjectTask;
import com.aimluck.eip.common.ALAbstractFormData;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.project.util.ProjectFormUtils;
import com.aimluck.eip.util.ALEipUtils;

/**
 *
 */
public class ProjectTaskProgressFormData extends ALAbstractFormData {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(ProjectTaskProgressFormData.class.getName());

  private String name = null;

  private String taskId = null;

  private String value = null;

  private Integer loginUserId = null;

  /**
   * 初期化します。
   * 
   * @param action
   *          ALAction
   * @param rundata
   *          RunData
   * @param context
   *          Context
   */
  @Override
  public void init(ALAction action, RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {
    super.init(action, rundata, context);
    name = rundata.getParameters().getString("name", "");
    taskId = rundata.getParameters().getString("taskId", "");
    value = rundata.getParameters().getString("value", "");
    loginUserId = ALEipUtils.getUserId(rundata);
  }

  /**
   * 各フィールドを初期化します。
   * 
   * 
   */
  @Override
  public void initField() {
  }

  /**
   * データに値を設定します。
   * 
   * @param rundata
   *          RunData
   * @param context
   *          Context
   * @param msgList
   *          エラーメッセージリスト
   * @return TRUE 成功 FALSE 失敗
   */
  @Override
  protected boolean setFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
    return true;
  }

  /**
   * 各フィールドに対する制約条件を設定します。
   */
  @Override
  protected void setValidator() {
  }

  /**
   * フォームに入力されたデータの妥当性検証を行います。
   * 
   * @param msgList
   *          エラーメッセージリスト
   * @return TRUE 成功 FALSE 失敗
   */
  @Override
  protected boolean validate(List<String> msgList) {
    // 進捗率更新
    if ("progress_rate".equals(name)) {
      if (StringUtils.isEmpty(taskId)
        || StringUtils.isEmpty(value)
        || !(StringUtils.isNumeric(taskId) && StringUtils.isNumeric(value))) {
        msgList.add(getl10n("PROJECT_UPDATE_ERROR"));
      }
      if (!ProjectFormUtils.isEditable(taskId, loginUserId)) {
        msgList.add(getl10n("PROJECT_TASK_NOT_UPDATE"));
      }
    }
    return msgList.isEmpty();
  }

  /**
   * 『項目定義』を読み込みます。
   * 
   * @param rundata
   *          RunData
   * @param context
   *          Context
   * @param msgList
   *          エラーメッセージリスト
   * @return TRUE 成功 FALSE 失敗
   */
  @Override
  protected boolean loadFormData(RunData rundata, Context context,
      List<String> msgList) {
    try {
      return true;
    } catch (Exception e) {
      logger.error("Exception", e);
      return false;
    }
  }

  /**
   * 『項目定義』を追加します。
   * 
   * @param rundata
   *          RunData
   * @param context
   *          Context
   * @param msgList
   *          エラーメッセージリスト
   * @return TRUE 成功 FALSE 失敗
   */
  @Override
  protected boolean insertFormData(RunData rundata, Context context,
      List<String> msgList) {
    return false;
  }

  /**
   * 『タスク』を更新します。
   * 
   * @param rundata
   *          RunData
   * @param context
   *          Context
   * @param msgList
   *          エラーメッセージリスト
   * @return TRUE 成功 FALSE 失敗
   */
  @Override
  protected boolean updateFormData(RunData rundata, Context context,
      List<String> msgList) {
    boolean res = true;
    try {
      if ("progress_rate".equals(name)) {
        EipTProjectTask task =
          Database.get(EipTProjectTask.class, Integer.valueOf(taskId));
        if (null == task) {
          msgList.add(getl10n("PROJECT_UPDATE_ERROR"));
          return false;
        }

        if ("100".equals(value)) {
          /** 100%の場合はステータスを終了にする */
          task.setStatus("4");
        } else if (task.getProgressRate().intValue() == 100
          && !"100".equals(value)) {
          /** 100%の状態から下がるときはステータスを進行中にする */
          task.setStatus("2");
        }

        task.setProgressRate(Integer.valueOf(value));

        task.setUpdateDate(new Date());

        // プロジェクト情報を更新
        ProjectFormUtils.updateProject(task.getProjectId(), ALEipUtils
          .getUserId(rundata));

        // タスクとプロジェクト情報の更新をコミット
        Database.commit();
      }
    } catch (Exception e) {
      Database.rollback();
      logger.error("Exception", e);
      res = false;
    }
    return res;
  }

  /**
   * 『タスク』を削除します。
   * 
   * @param rundata
   *          RunData
   * @param context
   *          Context
   * @param msgList
   *          エラーメッセージリスト
   * @return TRUE 成功 FALSE 失敗
   */
  @Override
  protected boolean deleteFormData(RunData rundata, Context context,
      List<String> msgList) {
    return false;
  }

}
