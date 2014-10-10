/*
] * Aipo is a groupware program developed by Aimluck,Inc.
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

package com.aimluck.eip.services.social.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.services.TurbineServices;

import com.aimluck.eip.cayenne.om.portlet.EipTTimeline;
import com.aimluck.eip.cayenne.om.portlet.EipTTimelineMap;
import com.aimluck.eip.cayenne.om.social.Activity;
import com.aimluck.eip.cayenne.om.social.ActivityMap;
import com.aimluck.eip.cayenne.om.social.AppData;
import com.aimluck.eip.cayenne.om.social.Application;
import com.aimluck.eip.cayenne.om.social.ContainerConfig;
import com.aimluck.eip.cayenne.om.social.ModuleId;
import com.aimluck.eip.cayenne.om.social.OAuthConsumer;
import com.aimluck.eip.common.ALActivity;
import com.aimluck.eip.common.ALActivityCount;
import com.aimluck.eip.common.ALApplication;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALEipManager;
import com.aimluck.eip.common.ALEipUser;
import com.aimluck.eip.common.ALOAuthConsumer;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.Operations;
import com.aimluck.eip.orm.query.ResultList;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.services.accessctl.ALAccessControlConstants;
import com.aimluck.eip.services.accessctl.ALAccessControlFactoryService;
import com.aimluck.eip.services.accessctl.ALAccessControlHandler;
import com.aimluck.eip.services.social.ALContainerConfigService;
import com.aimluck.eip.services.social.ALSocialApplicationConstants;
import com.aimluck.eip.services.social.ALSocialApplicationHandler;
import com.aimluck.eip.services.social.gadgets.ALGadgetSpec;
import com.aimluck.eip.services.social.gadgets.ALOAuthService;
import com.aimluck.eip.services.social.model.ALActivityGetRequest;
import com.aimluck.eip.services.social.model.ALActivityPutRequest;
import com.aimluck.eip.services.social.model.ALApplicationGetRequest;
import com.aimluck.eip.services.social.model.ALApplicationGetRequest.Status;
import com.aimluck.eip.services.social.model.ALApplicationPutRequest;
import com.aimluck.eip.services.social.model.ALOAuthConsumerPutRequest;
import com.aimluck.eip.util.ALCommonUtils;
import com.aimluck.eip.util.ALEipUtils;

/**
 *
 */
public class ALDefaultSocialApplicationHanlder extends
    ALSocialApplicationHandler {

  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(ALDefaultSocialApplicationHanlder.class.getName());

  private static ALSocialApplicationHandler instance;

  public static ALSocialApplicationHandler getInstance() {
    if (instance == null) {
      instance = new ALDefaultSocialApplicationHanlder();
    }
    return instance;
  }

  /**
   * @return
   */
  @Override
  public ResultList<ALApplication> getApplicationList(
      ALApplicationGetRequest request) {
    SelectQuery<Application> query = buildApplicationQuery(request);
    ResultList<Application> resultList = query.getResultList();
    List<ALApplication> list = new ArrayList<ALApplication>(resultList.size());
    List<String> specUrls = new ArrayList<String>(list.size());
    boolean fetchXml = request.isFetchXml();
    for (Application app : resultList) {
      specUrls.add(app.getUrl());
    }
    Map<String, ALGadgetSpec> metaData = null;
    if (fetchXml) {
      metaData = getMetaData(specUrls, "home", request.isDetail(), true);
    }
    for (Application app : resultList) {
      ALApplication model = new ALApplication();
      model.setAppId(String.valueOf(app.getId()));
      model.setTitle(app.getTitle());
      model.setConsumerKey(app.getConsumerKey());
      model.setConsumerSecret(app.getConsumerSecret());
      model.setUrl(app.getUrl());
      model.setStatus(app.getStatus());
      model.setDescription(app.getDescription());
      if (fetchXml) {
        ALGadgetSpec gadgetSpec = metaData.get(app.getUrl());
        model.setUserPrefs(gadgetSpec.getUserPrefs());
      }
      list.add(model);
    }
    ResultList<ALApplication> result =
      new ResultList<ALApplication>(list, resultList.getLimit(), resultList
        .getPage(), resultList.getTotalCount());
    return result;
  }

  /**
   * @param appId
   * @return
   */
  @Override
  public ALApplication getApplication(ALApplicationGetRequest request) {
    SelectQuery<Application> query = buildApplicationQuery(request);
    Application app = query.fetchSingle();
    if (app == null) {
      return null;
    }

    ALApplication model = new ALApplication();
    model.setAppId(String.valueOf(app.getId()));
    model.setTitle(app.getTitle());
    model.setConsumerKey(app.getConsumerKey());
    model.setConsumerSecret(app.getConsumerSecret());
    model.setUrl(app.getUrl());
    model.setStatus(app.getStatus());
    model.setIcon(app.getIcon());
    model.setDescription(app.getDescription());

    boolean fetchXml = request.isFetchXml();
    ALGadgetSpec gadgetSpec = null;
    if (fetchXml) {
      gadgetSpec = getMetaData(app.getUrl(), request.isDetail());
    }
    if (gadgetSpec != null) {
      model.setUserPrefs(gadgetSpec.getUserPrefs());
    }
    if (gadgetSpec != null && request.isDetail()) {
      List<ALOAuthConsumer> consumers = new ArrayList<ALOAuthConsumer>();
      List<ALOAuthService> services = gadgetSpec.getOAuthServices();
      @SuppressWarnings("unchecked")
      List<OAuthConsumer> consumerModels = app.getOauthConsumer();
      for (ALOAuthService service : services) {
        ALOAuthConsumer consumer = new ALOAuthConsumer();
        consumer.setAppId(app.getAppId());
        consumer.setName(service.getName());
        consumer.setAuthorizationUrl(service.getAuthorizationUrl());
        consumer.setRequestUrl(service.getRequestUrl());
        consumer.setAccessUrl(service.getAccessUrl());
        for (OAuthConsumer consumerModel : consumerModels) {
          if (service.getName().equals(consumerModel.getName())) {
            consumer.setType(consumerModel.getType());
            consumer.setConsumerKey(consumerModel.getConsumerKey());
            consumer.setConsumerSecret(consumerModel.getConsumerSecret());
          }
        }
        consumers.add(consumer);
      }
      model.addOAuthConsumers(consumers);
    }
    return model;
  }

  @Override
  public List<ALOAuthConsumer> getOAuthConsumer(String appId) {
    ALApplication app =
      getApplication(new ALApplicationGetRequest()
        .withAppId(appId)
        .withIsDetail(true)
        .withStatus(Status.ALL));
    return app.getOAuthConsumers();
  }

  @Override
  public void putOAuthConsumer(ALOAuthConsumerPutRequest request) {
    try {
      Date date = new Date();
      String appId = request.getAppId();
      String name = request.getName();
      Application app = Database.get(Application.class, "APP_ID", appId);
      if (app == null) {
        return;
      }
      @SuppressWarnings("unchecked")
      List<OAuthConsumer> oauthConsumers = app.getOauthConsumer();
      boolean has = false;
      if (oauthConsumers != null) {
        for (OAuthConsumer oauthConsumer : oauthConsumers) {
          if (oauthConsumer.getName().equals(name)) {
            oauthConsumer.setType(request.getType().value());
            oauthConsumer.setConsumerKey(request.getConsumerKey());
            oauthConsumer.setConsumerSecret(request.getConsumerSecret());
            oauthConsumer.setUpdateDate(date);
            has = true;
          }
        }
      }
      if (!has) {
        OAuthConsumer oauthConsumer = Database.create(OAuthConsumer.class);
        oauthConsumer.setApplication(app);
        oauthConsumer.setName(request.getName());
        oauthConsumer.setType(request.getType().value());
        oauthConsumer.setConsumerKey(request.getConsumerKey());
        oauthConsumer.setConsumerSecret(request.getConsumerSecret());
        oauthConsumer.setCreateDate(date);
        oauthConsumer.setUpdateDate(date);
      }

      Database.commit();
    } catch (Throwable t) {
      Database.rollback();
      throw new RuntimeException(t);
    }
  }

  /**
   * @param url
   */
  @Override
  public void createApplication(ALApplicationPutRequest request) {
    String url = request.getUrl();
    Date date = new Date();
    try {
      Application app = Database.create(Application.class);
      app.setAppId("");
      app.setUrl(url);
      app.setTitle(request.getTitle());
      // TODO: Restful API サポート時に ConsumerKey ConsumerSecret を発行する必要があります。
      // app.setConsumerKey(generateConsumerKey(url));
      // app.setConsumerSecret(generateConsumerSecret());
      app.setStatus(ALSocialApplicationConstants.STATUS_ACTIVE);
      app.setDescription(request.getDescription());
      app.setIcon(request.getIcon());
      app.setCreateDate(date);
      app.setUpdateDate(date);

      Database.commit();

      app.setAppId(String.valueOf(app.getId()));

      Database.commit();

      // Activity
      if (request.getActivityLoginName() != null
        && request.getActivityLoginName().length() > 0) {
        String title =
          new StringBuilder("アプリ「")
            .append(
              ALCommonUtils.compressString(request.getTitle().toString(), 30))
            .append("」をインストールしました。")
            .toString();
        String portletParams =
          new StringBuilder("?template=GadgetsAdminDetailScreen").append(
            "&entityid=").append(app.getAppId().toString()).toString();
        createActivity(new ALActivityPutRequest()
          .withAppId("GadgetAdd")
          .withPortletParams(portletParams)
          .withUserId(request.getActivityUserId())
          .withTitle(title)
          .withPriority(0f)
          .withLoginName(request.getActivityLoginName())
          .withExternalId(String.valueOf(app.getAppId().toString())));
      }

    } catch (Throwable t) {
      Database.rollback();
      throw new RuntimeException(t);
    }
  }

  /**
   * 
   * @param appId
   * @param request
   */
  @Override
  public void updateApplication(String appId, ALApplicationPutRequest request) {
    Date date = new Date();
    try {
      Application app = Database.get(Application.class, "APP_ID", appId);
      app.setTitle(request.getTitle());
      app.setDescription(request.getDescription());
      app.setIcon(request.getIcon());
      app.setUpdateDate(date);
      Database.commit();
    } catch (Throwable t) {
      Database.rollback();
      throw new RuntimeException(t);
    }
  }

  /**
   * 
   * @param appIdList
   */
  @Override
  public void enableApplication(String... appIdList) {
    try {
      for (String appId : appIdList) {
        Application app = Database.get(Application.class, "APP_ID", appId);
        if (app != null) {
          app.setStatus(1);
        }
      }
      Database.commit();
    } catch (Throwable t) {
      Database.rollback();
      throw new RuntimeException(t);
    }
  }

  /**
   * 
   * @param appIdList
   */
  @Override
  public void enableApplication(List<String> appIdList) {
    enableApplication(appIdList.toArray(new String[appIdList.size()]));
  }

  /**
   * 
   * @param appIdList
   */
  @Override
  public void disableApplication(String... appIdList) {
    try {
      for (String appId : appIdList) {
        Application app = Database.get(Application.class, "APP_ID", appId);
        if (app != null) {
          app.setStatus(0);
        }
      }
      Database.commit();
    } catch (Throwable t) {
      Database.rollback();
      throw new RuntimeException(t);
    }
  }

  /**
   * 
   * @param appIdList
   */
  @Override
  public void disableApplication(List<String> appIdList) {
    disableApplication(appIdList.toArray(new String[appIdList.size()]));
  }

  /**
   * 
   * @param appIdList
   */
  @Override
  public void deleteApplication(String... appIdList) {
    try {
      for (String appId : appIdList) {
        Database.delete(Database.get(Application.class, "APP_ID", appId));
        String sql1 = "delete from app_data where app_id = #bind($appId)";
        Database.sql(AppData.class, sql1).param("appId", appId).execute();

        String sql2 = "delete from activity where app_id = #bind($appId)";
        Database.sql(Activity.class, sql2).param("appId", appId).execute();
      }
      Database.commit();
    } catch (Throwable t) {
      Database.rollback();
      throw new RuntimeException(t);
    }
  }

  /**
   * @param appIdList
   */
  @Override
  public void deleteApplication(List<String> appIdList) {
    deleteApplication(appIdList.toArray(new String[appIdList.size()]));
  }

  @Override
  public boolean checkApplicationAvailability(String appId) {
    try {
      Application app = Database.get(Application.class, "APP_ID", appId);
      if (app == null) {
        return false;
      }
      Integer status = app.getStatus();
      if (status == null) {
        return false;
      }
      return status.intValue() == 1;
    } catch (Throwable t) {
      // logger.warn("[ALDefaultSocialApplicationHanlder]", t);
      return false;
    }
  }

  protected SelectQuery<Application> buildApplicationQuery(
      ALApplicationGetRequest request) {
    SelectQuery<Application> query = Database.query(Application.class);
    int limit = request.getLimit();
    int page = request.getPage();
    Status status = request.getStatus();
    if (limit > 0) {
      query.limit(limit);
    }
    if (page > 0) {
      query.page(page);
    }
    switch (status) {
      case ACTIVE:
        query.where(Operations.eq(Application.STATUS_PROPERTY, 1));
        break;
      case INACTIVE:
        query.where(Operations.eq(Application.STATUS_PROPERTY, 0));
        break;
      default:
        // ignore
    }
    String appId = request.getAppId();
    if (appId != null && appId.length() > 0) {
      query.where(Operations.eq(Application.APP_ID_PROPERTY, appId));
    }
    query.orderAscending(Application.TITLE_PROPERTY);
    return query;
  }

  /**
   * 
   * @param property
   * @return
   */
  @Override
  public String getContainerConfig(Property property) {
    ContainerConfig config = null;
    Object obj =
      ALEipManager.getInstance().getContainerConfig(property.toString());
    if (obj != null) {
      return (String) obj;
    } else {
      config =
        Database
          .query(ContainerConfig.class)
          .where(
            Operations.eq(ContainerConfig.NAME_PROPERTY, property.toString()))
          .fetchSingle();
      if (config == null) {
        ALEipManager.getInstance().setContainerConfig(
          property.toString(),
          property.defaultValue());
        return property.defaultValue();
      } else {
        ALEipManager.getInstance().setContainerConfig(
          property.toString(),
          config.getValue());
        return config.getValue();
      }
    }
  }

  /**
   * 
   * @param property
   * @param value
   */
  @Override
  public void putContainerConfig(Property property, String value) {
    try {
      ContainerConfig config =
        Database
          .query(ContainerConfig.class)
          .where(
            Operations.eq(ContainerConfig.NAME_PROPERTY, property.toString()))
          .fetchSingle();
      if (config == null) {
        config = Database.create(ContainerConfig.class);
        config.setName(property.toString());
      }
      config.setValue(value);
      Database.commit();
    } catch (Throwable t) {
      Database.rollback();
      throw new RuntimeException(t);
    }
  }

  @Override
  public ResultList<ALActivity> getActivityList(ALActivityGetRequest request) {

    SelectQuery<Activity> query = buildActivityQuery(request);
    ResultList<Activity> resultList = query.getResultList();
    List<ALActivity> list = new ArrayList<ALActivity>(resultList.size());
    for (Activity model : resultList) {
      ALActivity activity = new ALActivity();
      activity.setId(model.getId());
      activity.setLoginName(model.getLoginName());
      activity.setAppId(model.getAppId());
      activity.setTitle(model.getTitle());
      activity.setUpdateDate(model.getUpdateDate());
      activity.setExternalId(model.getExternalId());
      activity.setPortletParams(model.getPortletParams());
      activity.setIcon(model.getIcon());
      activity.setModuleId(model.getModuleId());
      try {
        ALEipUser user = ALEipUtils.getALEipUser(model.getLoginName());
        if (model.getAppId().equals("timeline") && user == null) {
          activity.setDisplayName(model.getLoginName());
        } else {
          activity.setDisplayName(user.getAliasName().getValue());

        }
      } catch (Throwable t) {
        //
      }
      String loginName = request.getTargetLoginName();
      if (loginName != null && loginName.length() > 0) {
        activity.setRead(isReadActivity(model.getId(), loginName));
      } else {
        activity.setRead(true);
      }
      list.add(activity);
    }
    ResultList<ALActivity> result =
      new ResultList<ALActivity>(list, resultList.getLimit(), resultList
        .getPage(), resultList.getTotalCount());
    return result;
  }

  @Override
  public ALActivity getActivity(ALActivityGetRequest request) {
    SelectQuery<Activity> query = buildActivityQuery(request);
    Activity model = query.fetchSingle();
    if (model == null) {
      return null;
    }
    ALActivity activity = new ALActivity();
    activity.setId(model.getId());
    activity.setAppId(model.getAppId());
    activity.setLoginName(model.getLoginName());
    activity.setTitle(model.getTitle());
    activity.setUpdateDate(model.getUpdateDate());
    activity.setExternalId(model.getExternalId());
    activity.setPortletParams(model.getPortletParams());
    activity.setIcon(model.getIcon());
    String loginName = request.getTargetLoginName();
    if (loginName != null && loginName.length() > 0) {
      activity.setRead(isReadActivity(model.getId(), loginName));
    } else {
      activity.setRead(false);
    }
    try {
      ALEipUser user = ALEipUtils.getALEipUser(model.getLoginName());
      activity.setDisplayName(user.getAliasName().getValue());
    } catch (ALDBErrorException e) {
      //
    }
    return activity;
  }

  @Override
  public ALActivityCount getActivityCount(ALActivityGetRequest request) {
    ALActivityCount result = new ALActivityCount();
    result.setCount(buildActivityQuery(request).getCount());
    request.setRead(-1);
    Activity activity = buildActivityQuery(request).limit(1).fetchSingle();
    if (activity != null) {
      result.setMax(activity.getUpdateDate().getTime());
    }
    return result;
  }

  @Override
  public void setAllReadActivity(String loginName) {

    try {
      DataContext dataContext = DataContext.getThreadDataContext();
      String url =
        dataContext
          .getParentDataDomain()
          .getNode(Database.getDomainName() + "domainNode")
          .getDataSource()
          .getConnection()
          .getMetaData()
          .getURL();

      String sql = "";
      if (url != null && url.startsWith("jdbc:postgresql")) {
        StringBuilder b =
          new StringBuilder("update activity_map set is_read = 1 ");
        b
          .append(" from activity where activity_map.activity_id = activity.id ");
        b.append(" and activity_map.login_name = #bind($loginName) ");
        sql = b.toString();
      } else {
        StringBuilder b =
          new StringBuilder("update activity_map, activity set is_read = 1 ");
        b.append(" where activity_map.activity_id = activity.id ");
        b.append(" and activity_map.login_name = #bind($loginName) ");
        sql = b.toString();
      }

      Database
        .sql(ActivityMap.class, sql)
        .param("loginName", loginName)
        .execute();
    } catch (Throwable t) {
      Database.rollback();
      logger.warn("[ALDefaultSocialApplicationHanlder]", t);
    }
  }

  @Override
  public void setReadActivity(int activityId, String loginName) {

    try {

      DataContext dataContext = DataContext.getThreadDataContext();
      String url =
        dataContext
          .getParentDataDomain()
          .getNode(Database.getDomainName() + "domainNode")
          .getDataSource()
          .getConnection()
          .getMetaData()
          .getURL();

      String sql = "";
      if (url != null && url.startsWith("jdbc:postgresql")) {
        StringBuilder b =
          new StringBuilder("update activity_map set is_read = 1 ");
        b
          .append(" from activity where activity_map.activity_id = activity.id ");
        b.append(" and activity.id = #bind($activityId) ");
        b.append(" and activity_map.login_name = #bind($loginName) ");
        sql = b.toString();
      } else {
        StringBuilder b =
          new StringBuilder("update activity_map, activity set is_read = 1 ");
        b.append(" where activity_map.activity_id = activity.id ");
        b.append(" and activity.id = #bind($activityId) ");
        b.append(" and activity_map.login_name = #bind($loginName) ");
        sql = b.toString();
      }

      Database
        .sql(ActivityMap.class, sql)
        .param("activityId", activityId)
        .param("loginName", loginName)
        .execute();
    } catch (Throwable t) {
      Database.rollback();
      logger.warn("[ALDefaultSocialApplicationHanlder]", t);
    }
  }

  public boolean isReadActivity(int activityId, String loginName) {
    StringBuilder b =
      new StringBuilder(
        "select activity_map.is_read from activity_map inner join activity on activity_map.activity_id = activity.id ");
    b.append(" and activity.id = #bind($activityId) ");
    b.append(" and activity_map.login_name = #bind($loginName) ");
    String sql = b.toString();

    try {
      ActivityMap activityMap =
        Database
          .sql(ActivityMap.class, sql)
          .param("activityId", activityId)
          .param("loginName", loginName)
          .fetchSingle();
      if (activityMap != null) {
        return activityMap.getIsRead().intValue() == 1;
      }
    } catch (Throwable t) {
      Database.rollback();
      logger.warn("[ALDefaultSocialApplicationHanlder]", t);
    }
    return true;
  }

  protected SelectQuery<Activity> buildActivityQuery(
      ALActivityGetRequest request) {

    ALAccessControlFactoryService aclservice =
      (ALAccessControlFactoryService) ((TurbineServices) TurbineServices
        .getInstance()).getService(ALAccessControlFactoryService.SERVICE_NAME);
    ALAccessControlHandler aclhandler = aclservice.getAccessControlHandler();

    SelectQuery<Activity> query = Database.query(Activity.class);
    int limit = request.getLimit();
    if (limit > 0) {
      query.limit(limit);
    }
    int page = request.getPage();
    if (page > 0) {
      query.page(page);
    }
    int isRead = request.isRead();
    if (isRead >= 0) {
      query.where(Operations.eq(Activity.ACTIVITY_MAPS_PROPERTY
        + "."
        + ActivityMap.IS_READ_PROPERTY, isRead));
    }
    int external_id = request.getExternalId();

    if (external_id > 0) {
      query.where(Operations.eq(Activity.EXTERNAL_ID_PROPERTY, external_id));
    }

    float priority = request.getPriority();
    if (priority >= 0f) {
      query.where(Operations.eq(Activity.PRIORITY_PROPERTY, priority));
    }
    String keyword = request.getKeyword();
    if ((keyword != null) && (!keyword.equals(""))) {
      // 選択したキーワードを指定する．
      query.where(Operations.contains(Activity.TITLE_PROPERTY, keyword).or(
        Operations.contains(Activity.LOGIN_NAME_PROPERTY, keyword)));
    }
    String loginName = request.getLoginName();
    if (loginName != null && loginName.length() > 0) {

      ALEipUser user = null;
      try {
        user = ALEipUtils.getALEipUser(loginName);
      } catch (ALDBErrorException e) {
        throw new RuntimeException(e);
      }

      if (priority < 1f) {
        if (!aclhandler.hasAuthority(
          (int) user.getUserId().getValue(),
          ALAccessControlConstants.POERTLET_FEATURE_MSGBOARD_TOPIC,
          ALAccessControlConstants.VALUE_ACL_LIST)) {
          query.where(Operations.ne(Activity.APP_ID_PROPERTY, "Msgboard"));
        }

        if (!aclhandler.hasAuthority(
          (int) user.getUserId().getValue(),
          ALAccessControlConstants.POERTLET_FEATURE_BLOG_ENTRY_OTHER,
          ALAccessControlConstants.VALUE_ACL_LIST)) {
          query.where(Operations.ne(Activity.APP_ID_PROPERTY, "blog"));
        }
      }

      query.where(Operations.ne(Activity.LOGIN_NAME_PROPERTY, loginName));
    }
    String targetLoginName = request.getTargetLoginName();
    boolean targetLoginNameLimit = request.isTargetLoginNameLimit();
    if (targetLoginName != null && targetLoginName.length() > 0) {
      if (!targetLoginNameLimit) {
        // 更新情報
        query.where(Operations.in(Activity.ACTIVITY_MAPS_PROPERTY
          + "."
          + ActivityMap.LOGIN_NAME_PROPERTY, targetLoginName, "-1"));

      } else {
        // あなた(自分)宛のお知らせ
        query.where(Operations.in(Activity.ACTIVITY_MAPS_PROPERTY
          + "."
          + ActivityMap.LOGIN_NAME_PROPERTY, targetLoginName));
      }
    }
    String appId = request.getAppId();
    if (appId != null && appId.length() > 0) {
      if (appId.equals("Gadget")) {
        query.where(Operations.ne(Activity.APP_ID_PROPERTY, "Schedule"));
        query.where(Operations.ne(Activity.APP_ID_PROPERTY, "Blog"));
        query.where(Operations.ne(Activity.APP_ID_PROPERTY, "Msgboard"));
        query.where(Operations.ne(Activity.APP_ID_PROPERTY, "ToDo"));
        query.where(Operations.ne(Activity.APP_ID_PROPERTY, "Cabinet"));
      } else {
        query.where(Operations.eq(Activity.APP_ID_PROPERTY, appId));
      }
    }

    String postId = request.getPostId();
    if (postId != null && !"".equals(postId)) {
      List<Integer> userIds = ALEipUtils.getUserIds(postId);
      List<String> loginNameList = new ArrayList<String>();
      for (int userId : userIds) {
        try {
          String name = ALEipUtils.getALEipUser(userId).getName().getValue();

          loginNameList.add(name);
        } catch (ALDBErrorException e) {

        }
      }
      if (!loginNameList.isEmpty()) {
        query.where(Operations.in(Activity.LOGIN_NAME_PROPERTY, loginNameList));
      }

    }

    long max = request.getMax();
    if (max > 0) {
      Date date = new Date();
      date.setTime(max);
      query.where(Operations.gt(Activity.UPDATE_DATE_PROPERTY, date));
    }
    query.orderDesending(Activity.UPDATE_DATE_PROPERTY);
    return query;
  }

  /**
   * @param request
   */
  @Override
  public void createActivity(ALActivityPutRequest request) {
    try {
      Activity activity = Database.create(Activity.class);
      activity.setAppId(request.getAppId());
      activity.setLoginName(request.getLoginName());
      activity.setBody(request.getBody());
      activity.setExternalId(request.getExternalId());
      activity.setModuleId(0);

      // priority は 0 <= 1 の間
      Float priority = request.getPriority();
      if (priority < 0) {
        priority = 0f;
      }
      if (priority > 1) {
        priority = 1f;
      }
      activity.setPriority(priority);
      activity.setTitle(request.getTitle());
      activity.setPortletParams(request.getPortletParams());
      activity.setUpdateDate(new Date());

      List<String> recipients = request.getRecipients();
      if (recipients != null && recipients.size() > 0) {
        for (String recipient : recipients) {
          ActivityMap activityMap = Database.create(ActivityMap.class);
          activityMap.setLoginName(recipient);
          activityMap.setActivity(activity);
          activityMap.setIsRead(priority == 1f ? 0 : 1);
        }
      } else {
        ActivityMap activityMap = Database.create(ActivityMap.class);
        activityMap.setLoginName("-1");
        activityMap.setActivity(activity);
        activityMap.setIsRead(1);
      }

      ALApplication application =
        getApplication(new ALApplicationGetRequest().withAppId(request
          .getAppId()));
      if (application != null) {
        activity.setIcon(application.getIcon().getValue());
      }

      String activitySaveLimit =
        ALContainerConfigService.get(Property.ACTIVITY_SAVE_LIMIT);
      int limit = 30;
      try {
        limit = Integer.valueOf(activitySaveLimit).intValue();
      } catch (Throwable ignore) {

      }
      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.DAY_OF_MONTH, -limit);

      Database.query(ActivityMap.class).where(
        Operations.lt(ActivityMap.ACTIVITY_PROPERTY
          + "."
          + Activity.UPDATE_DATE_PROPERTY, cal.getTime())).deleteAll();

      Database.commit();

      // タイムラインに更新情報を追加
      if (priority == 0f) {
        Calendar tCal = Calendar.getInstance();
        int parentId = 0;
        Calendar tCalBefore = Calendar.getInstance();
        tCalBefore.set(tCal.get(Calendar.YEAR), tCal.get(Calendar.MONTH), tCal
          .get(Calendar.DATE), 0, 0, 0);
        Calendar tCalAfter = Calendar.getInstance();
        tCalAfter.set(tCal.get(Calendar.YEAR), tCal.get(Calendar.MONTH), tCal
          .get(Calendar.DATE), 0, 0, 0);
        tCalAfter.add(Calendar.DATE, 1);

        Expression exp1 =
          ExpressionFactory.matchExp(EipTTimeline.OWNER_ID_PROPERTY, Integer
            .valueOf(request.getUserId()));
        Expression exp2 =
          ExpressionFactory.matchExp(
            EipTTimeline.TIMELINE_TYPE_PROPERTY,
            EipTTimeline.TIMELINE_TYPE_ACTIVITY);
        Expression exp3 =
          ExpressionFactory.matchExp(EipTTimeline.PARENT_ID_PROPERTY, 0);
        Expression exp4 =
          ExpressionFactory.betweenExp(
            EipTTimeline.CREATE_DATE_PROPERTY,
            tCalBefore.getTime(),
            tCalAfter.getTime());
        Expression exp5 =
          ExpressionFactory.matchExp(
            EipTTimeline.APP_ID_PROPERTY,
            "ACTIVITY_PARENT");
        Expression exp6 =
          ExpressionFactory.matchExp(EipTTimeline.EXTERNAL_ID_PROPERTY, "0");
        SelectQuery<EipTTimeline> tQuery = Database.query(EipTTimeline.class);
        tQuery.andQualifier(exp1.andExp(exp2.andExp(exp3.andExp(exp4.andExp(
          exp5).andExp(exp6)))));
        tQuery.distinct(true);
        List<EipTTimeline> parents = tQuery.fetchList();
        if (parents != null && parents.size() != 0) {
          parentId = parents.get(0).getTimelineId();
          EipTTimeline parent =
            Database.get(EipTTimeline.class, Integer.valueOf(parentId));
          parent.setUpdateDate(tCal.getTime());
        } else {
          // 親ダミー生成
          EipTTimeline timeline = Database.create(EipTTimeline.class);
          timeline.setParentId(0);
          timeline.setOwnerId(request.getUserId());
          timeline.setAppId("ACTIVITY_PARENT");
          timeline.setExternalId("0");
          timeline.setNote("");
          timeline.setTimelineType(EipTTimeline.TIMELINE_TYPE_ACTIVITY);
          timeline.setParams(new SimpleDateFormat("yyyyMMdd").format(cal
            .getTime()));
          // 作成日
          timeline.setCreateDate(tCal.getTime());
          // 更新日
          timeline.setUpdateDate(tCal.getTime());

          Database.commit();

          this.daysFirstActivate(request.getLoginName());

          ALApplication application2 =
            getApplication(new ALApplicationGetRequest().withAppId(request
              .getAppId()));
          if (application2 != null) {
            activity.setIcon(application2.getIcon().getValue());
          }

          String activitySaveLimit2 =
            ALContainerConfigService.get(Property.ACTIVITY_SAVE_LIMIT);
          int limit2 = 30;
          try {
            limit2 = Integer.valueOf(activitySaveLimit2).intValue();
          } catch (Throwable ignore) {

          }
          Calendar cal2 = Calendar.getInstance();
          cal2.add(Calendar.DAY_OF_MONTH, -limit2);

          Database.query(EipTTimelineMap.class).where(
            Operations.lt(EipTTimelineMap.EIP_TTIMELINE_PROPERTY
              + "."
              + EipTTimeline.UPDATE_DATE_PROPERTY, cal2.getTime())).deleteAll();

          // 親データ再検索
          tQuery = Database.query(EipTTimeline.class);
          tQuery.andQualifier(exp1.andExp(exp2.andExp(exp3.andExp(exp4.andExp(
            exp5).andExp(exp6)))));
          tQuery.distinct(true);
          parents = tQuery.fetchList();
          if (parents != null && parents.size() != 0) {
            parentId = parents.get(0).getTimelineId();
            EipTTimeline parent =
              Database.get(EipTTimeline.class, Integer.valueOf(parentId));
            parent.setUpdateDate(tCal.getTime());
          }
        }
        exp1 =
          ExpressionFactory.matchExp(
            EipTTimeline.TIMELINE_TYPE_PROPERTY,
            EipTTimeline.TIMELINE_TYPE_ACTIVITY);
        exp2 =
          ExpressionFactory.matchExp(EipTTimeline.OWNER_ID_PROPERTY, Integer
            .valueOf(request.getUserId()));
        exp3 =
          ExpressionFactory.matchExp(EipTTimeline.APP_ID_PROPERTY, request
            .getAppId());
        exp4 =
          ExpressionFactory.matchExp(EipTTimeline.EXTERNAL_ID_PROPERTY, request
            .getExternalId());
        tQuery = Database.query(EipTTimeline.class);
        tQuery.andQualifier(exp1.andExp(exp2.andExp(exp3.andExp(exp4))));
        // tQuery.andQualifier(exp3);
        tQuery.distinct(true);
        List<EipTTimeline> olders = tQuery.fetchList();
        if (olders != null && olders.size() != 0) {
          SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
          String timelineSql =
            "UPDATE eip_t_timeline"
              + " SET parent_id = #bind($parentId) "
              + ", note = #bind($title) "
              + ", params = #bind($portletParams) "
              + ", update_date =  '"
              + sdf.format(tCal.getTime())
              + "'"
              + " WHERE timeline_type = 'A' "
              + " AND owner_id = #bind($userId) "
              + " AND app_id = #bind($appId) "
              + " AND external_id = #bind($externalId); ";
          Database
            .sql(EipTTimeline.class, timelineSql)
            .param("parentId", parentId)
            .param("title", request.getTitle())
            .param("portletParams", request.getPortletParams())
            .param("userId", request.getUserId())
            .param("appId", request.getAppId())
            .param("externalId", request.getExternalId())
            .execute();
        } else {
          EipTTimeline timeline = Database.create(EipTTimeline.class);
          timeline.setParentId(parentId);
          timeline.setOwnerId(request.getUserId());
          timeline.setAppId(request.getAppId());
          timeline.setExternalId(request.getExternalId());
          timeline.setNote(request.getTitle());
          timeline.setTimelineType(EipTTimeline.TIMELINE_TYPE_ACTIVITY);
          timeline.setParams(request.getPortletParams());
          // 作成日
          timeline.setCreateDate(tCal.getTime());
          // 更新日
          timeline.setUpdateDate(tCal.getTime());

          // タイムラインマップ追加
          if (recipients != null && recipients.size() > 0) {
            for (String recipient : recipients) {
              EipTTimelineMap timelineMap =
                Database.create(EipTTimelineMap.class);
              timelineMap.setLoginName(recipient);
              timelineMap.setEipTTimeline(timeline);
              timelineMap.setIsRead(priority == 1f ? 0 : 1);
            }
          } else {
            EipTTimelineMap timelineMap =
              Database.create(EipTTimelineMap.class);
            timelineMap.setLoginName("-1");
            timelineMap.setEipTTimeline(timeline);
            timelineMap.setIsRead(1);
          }
        }
        Database.commit();
      }
    } catch (Throwable t) {
      Database.rollback();
      throw new RuntimeException(t);
    }
  }

  /**
   * @return
   */
  @Override
  public long getNextModuleId() {
    ModuleId moduleId = null;
    try {
      moduleId = Database.create(ModuleId.class);
      Database.commit();
    } catch (Throwable t) {
      Database.rollback();
      throw new RuntimeException(t);

    }
    long next = moduleId.getId().longValue();
    try {
      String sql = "delete from module_id";
      Database.sql(ModuleId.class, sql).execute();
    } catch (Throwable t) {
      Database.rollback();
      // ignore
    }
    return next;
  }

  @Override
  public void deleteUserData(String... loginNameList) {
    try {
      for (String loginName : loginNameList) {
        String sql1 =
          "delete from app_data where login_name = #bind($loginName)";
        Database
          .sql(AppData.class, sql1)
          .param("loginName", loginName)
          .execute();

        String sql2 =
          "delete from activity where login_name = #bind($loginName)";
        Database
          .sql(Activity.class, sql2)
          .param("loginName", loginName)
          .execute();

        String sql3 =
          "delete from activity_map where login_name = #bind($loginName)";
        Database
          .sql(Activity.class, sql3)
          .param("loginName", loginName)
          .execute();
      }
      Database.commit();
    } catch (Throwable t) {
      Database.rollback();
      throw new RuntimeException(t);
    }
  }

  @Override
  public void deleteUserData(List<String> loginNameList) {
    deleteUserData(loginNameList.toArray(new String[loginNameList.size()]));
  }

  /**
   *
   */
  @Override
  protected void daysFirstActivate(String uid) {
    // noop
  }
}
