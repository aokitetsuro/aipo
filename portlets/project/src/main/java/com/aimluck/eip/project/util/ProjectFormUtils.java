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
 *
 * Project Management Portlet was developed by Advance,Inc.
 * http://www.a-dvance.co.jp/
 */

package com.aimluck.eip.project.util;

import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;

import com.aimluck.eip.cayenne.om.portlet.EipTProject;
import com.aimluck.eip.cayenne.om.portlet.EipTProjectTask;
import com.aimluck.eip.cayenne.om.portlet.EipTProjectTaskMember;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.Operations;

/**
 * プロジェクト管理のユーティリティクラスです。
 * 
 */
public class ProjectFormUtils {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(ProjectFormUtils.class.getName());

  /**
   * プロジェクトを更新する
   * 
   * @param projectId
   *          プロジェクトID
   * @return TRUE:更新実行 FALSE:更新しない
   */
  public static void updateProject(Integer projectId, Integer loginUserId) {

    // オブジェクトモデルを取得
    EipTProject project = ProjectUtils.getEipTProject(projectId);
    if (project == null) {
      return;
    }

    if (ProjectUtils.FLG_OFF.equals(project.getProgressFlg())) {
      // 自動計算しない場合
      return;
    }

    project.setProgressRate(ProjectUtils.getProjectProgressRate(projectId));
    project.setUpdateUserId(loginUserId);
    project.setUpdateDate(Calendar.getInstance().getTime());

    Database.commit();
  }

  /**
   * task member or project admin able to update
   * 
   * @return
   */
  public static boolean isEditable(Integer taskId, Integer userId) {
    EipTProjectTask task = Database.get(EipTProjectTask.class, taskId);
    EipTProject project = Database.get(EipTProject.class, task.getProjectId());
    if (null == project) {
      return false;
    }
    if (project.getAdminUserId().equals(userId)) {
      return true;
    }
    List<EipTProjectTaskMember> list =
      Database
        .query(EipTProjectTaskMember.class)
        .where(Operations.eq(EipTProjectTaskMember.TASK_ID_PROPERTY, taskId))
        .fetchList();
    for (EipTProjectTaskMember member : list) {
      if (member.getUserId().equals(userId)) {
        return true;
      }
    }
    return false;
  }

  public static boolean isEditable(String taskId, Integer userId) {
    if (StringUtils.isEmpty(taskId)
      || !StringUtils.isNumeric(taskId)
      || null == userId) {
      return false;
    }
    return isEditable(Integer.valueOf(taskId), userId);
  }

}
