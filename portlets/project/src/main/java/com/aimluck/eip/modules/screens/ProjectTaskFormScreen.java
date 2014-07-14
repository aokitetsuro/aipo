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

package com.aimluck.eip.modules.screens;

import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.eip.project.ProjectTaskFormData;
import com.aimluck.eip.util.ALEipUtils;

/**
 *
 */
public class ProjectTaskFormScreen extends ProjectScreen {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(ProjectTaskFormScreen.class.getName());

  /**
   * 出力する
   * 
   * @param rundata
   *          RunData
   * @param context
   *          Context
   * @throws Exception
   *           例外
   */
  @Override
  protected void doOutput(RunData rundata, Context context) throws Exception {
    try {
      doTask_form(rundata, context);
    } catch (Exception ex) {
      logger.error("[ProjectTaskFormScreen] Exception.", ex);
      ALEipUtils.redirectDBError(rundata);
    }
  }

  /**
   * 登録・編集画面用入力フォーム表示
   * 
   * @param rundata
   *          RunData
   * @param context
   *          Context
   */
  protected void doTask_form(RunData rundata, Context context) {
    ProjectTaskFormData formData = new ProjectTaskFormData();
    formData.initField();
    formData.doViewForm(this, rundata, context);
    if (formData.getMode().equals("new_form")) {
      context.put("new_form", true);
    } else {
      context.put("new_form", false);
    }

    String layout_template = "portlets/html/ja/ajax-project-task-form.vm";
    setTemplate(rundata, context, layout_template);
  }

}
