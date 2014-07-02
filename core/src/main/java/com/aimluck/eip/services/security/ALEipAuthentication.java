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

package com.aimluck.eip.services.security;

import java.util.Date;
import java.util.GregorianCalendar;

import javax.servlet.ServletConfig;

import org.apache.jetspeed.om.security.JetspeedUser;
import org.apache.jetspeed.om.security.UserNamePrincipal;
import org.apache.jetspeed.services.JetspeedSecurity;
import org.apache.jetspeed.services.JetspeedUserManagement;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.jetspeed.services.rundata.JetspeedRunData;
import org.apache.jetspeed.services.rundata.JetspeedRunDataService;
import org.apache.jetspeed.services.security.CredentialExpiredException;
import org.apache.jetspeed.services.security.FailedLoginException;
import org.apache.jetspeed.services.security.JetspeedSecurityCache;
import org.apache.jetspeed.services.security.JetspeedSecurityException;
import org.apache.jetspeed.services.security.JetspeedSecurityService;
import org.apache.jetspeed.services.security.LoginException;
import org.apache.jetspeed.services.security.PortalAuthentication;
import org.apache.jetspeed.services.security.UnknownUserException;
import org.apache.turbine.services.InitializationException;
import org.apache.turbine.services.TurbineBaseService;
import org.apache.turbine.services.TurbineServices;
import org.apache.turbine.services.resources.ResourceService;
import org.apache.turbine.services.rundata.RunDataService;

import com.aimluck.eip.common.ALBaseUser;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.util.ALCommonUtils;

/**
 * ライセンス検証のためのクラスです。 <br />
 * 
 */
public class ALEipAuthentication extends TurbineBaseService implements
    PortalAuthentication {

  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(ALEipAuthentication.class.getName());

  private JetspeedRunDataService runDataService = null;

  private final static String CONFIG_ANONYMOUS_USER = "user.anonymous";

  String anonymousUser = "anon";

  private final static String CACHING_ENABLE = "caching.enable";

  private boolean cachingEnable = true;

  private final static String CONFIG_PASSWORD_EXPIRATION_PERIOD =
    "password.expiration.period";

  private int expirationPeriod = 0;

  /**
   * 
   * @param username
   * @param cellular_uid
   * @return
   * @throws UnknownUserException
   * @throws JetspeedSecurityException
   */
  public JetspeedUser loginCellularUid(String username, String cellular_uid)
      throws UnknownUserException, JetspeedSecurityException {
    JetspeedUser user =
      JetspeedUserManagement.getUser(new UserNamePrincipal(username));
    ALBaseUser baseUser = (ALBaseUser) user;
    if (cellular_uid == null
      || cellular_uid.length() == 0
      || baseUser == null
      || !cellular_uid.equals(baseUser.getCelluarUId())) {
      logger.error("Invalid cellular uid for user: " + username);
      throw new UnknownUserException(
        "[ALEipAuthentication] Credential authentication failure");
    }

    return user;
  }

  /**
   *
   */
  @Override
  public JetspeedUser login(String username, String password)
      throws LoginException {

    if (username.equals(this.anonymousUser)) {
      throw new LoginException("Anonymous user cannot login");
    }

    JetspeedUser user = null;

    username = JetspeedSecurity.convertUserName(username);
    password = JetspeedSecurity.convertPassword(password);

    if (password.startsWith(ALEipConstants.KEY_CELLULAR_UID)) {
      // 携帯電話の固有 ID でログイン認証する．
      String cellularUid =
        password.substring(ALEipConstants.KEY_CELLULAR_UID.length(), password
          .length());
      try {
        user = loginCellularUid(username, cellularUid);
      } catch (UnknownUserException e) {
        logger.warn("Unknown user attempted access: " + username, e);
        throw new FailedLoginException(e.toString());
      } catch (JetspeedSecurityException e) {
        logger.warn("User denied authentication: " + username, e);
        throw new LoginException(e.toString());
      }
    } else {
      try {
        user = JetspeedUserManagement.getUser(new UserNamePrincipal(username));
        password = JetspeedSecurity.encryptPassword(password);
      } catch (UnknownUserException e) {
        logger.warn("Unknown user attempted access: " + username, e);
        throw new FailedLoginException(e.toString());
      } catch (JetspeedSecurityException e) {
        logger.warn("User denied authentication: " + username, e);
        throw new LoginException(e.toString());
      }
      if (user == null || !user.getPassword().equals(password)) {
        logger.error("Invalid password for user: " + username);
        throw new FailedLoginException("Credential authentication failure");
      }
    }

    if (ALEipConstants.USER_STAT_DISABLED.equals(user.getDisabled())) {
      logger.error("User deleted : " + username);
      throw new FailedLoginException(ALEipConstants.USER_STAT_DISABLED);
    } else if (ALEipConstants.USER_STAT_NUTRAL.equals(user.getDisabled())) {
      logger.error("User disabled : " + username);
      throw new FailedLoginException(ALEipConstants.USER_STAT_NUTRAL);
    }

    // Check for password expiration
    if (this.expirationPeriod > 0) {
      Date passwordLastChangedDate = user.getPasswordChanged();
      Date passwordExpireDate = null;
      if (passwordLastChangedDate != null) {
        GregorianCalendar gcal =
          (GregorianCalendar) GregorianCalendar.getInstance();
        gcal.setTime(passwordLastChangedDate);
        gcal.add(GregorianCalendar.DATE, this.expirationPeriod);
        passwordExpireDate = gcal.getTime();
        if (logger.isDebugEnabled()) {
          logger.debug("TurbineAuthentication: password last changed = "
            + passwordLastChangedDate.toString()
            + ", password expires = "
            + passwordExpireDate.toString());
        }
      }

      if (passwordExpireDate == null
        || (new Date().getTime() > passwordExpireDate.getTime())) {
        throw new CredentialExpiredException("Password expired");
      }

    }

    // IPA#70075625
    // Sesion Fixation 対策
    JetspeedRunData rundata = getRunData();
    if (rundata != null) {
      // Session ID を再発行する
      rundata.getSession().invalidate();
      rundata.setSession(rundata.getRequest().getSession(true));
    }
    //

    user.setHasLoggedIn(Boolean.TRUE);

    try {
      user.updateLastLogin();
      putUserIntoContext(user);
      if (cachingEnable) {
        JetspeedSecurityCache.load(username);
      }
    } catch (Exception e) {
      logger.error("Failed to update last login ", e);
      putUserIntoContext(JetspeedSecurity.getAnonymousUser());
      throw new LoginException("Failed to update last login ", e);
    }

    // for security
    if (rundata != null) {
      rundata.getUser().setTemp(
        ALEipConstants.SECURE_ID,
        ALCommonUtils.getSecureRandomString());
    }

    return user;
  }

  /**
   *
   */
  @Override
  public JetspeedUser getAnonymousUser() throws LoginException {
    JetspeedUser user = null;
    try {
      user =
        JetspeedUserManagement.getUser(new UserNamePrincipal(anonymousUser));
      user.setHasLoggedIn(Boolean.FALSE);
      putUserIntoContext(user);
      if (cachingEnable) {
        JetspeedSecurityCache.load(user.getUserName());
      }
    } catch (JetspeedSecurityException e) {
      logger.error("Failed to get anonymous user: ", e);
      throw new LoginException("Failed to get anonymous user: ", e);
    }
    return user;
  }

  /**
   *
   */
  @Override
  public void logout() throws LoginException {
    try {
      getAnonymousUser();
    } catch (Throwable ignore) {
      // ignore
    }
  }

  /**
   *
   */
  @Override
  public synchronized void init(ServletConfig conf)
      throws InitializationException {
    if (getInit()) {
      return;
    }

    super.init(conf);

    ResourceService serviceConf =
      ((TurbineServices) TurbineServices.getInstance())
        .getResources(JetspeedSecurityService.SERVICE_NAME);

    anonymousUser = serviceConf.getString(CONFIG_ANONYMOUS_USER, anonymousUser);
    cachingEnable = serviceConf.getBoolean(CACHING_ENABLE, cachingEnable);
    expirationPeriod = serviceConf.getInt(CONFIG_PASSWORD_EXPIRATION_PERIOD, 0);

    this.runDataService =
      (JetspeedRunDataService) TurbineServices.getInstance().getService(
        RunDataService.SERVICE_NAME);

    setInit(true);
  }

  protected JetspeedRunData getRunData() {
    JetspeedRunData rundata = null;
    if (this.runDataService != null) {
      rundata = this.runDataService.getCurrentRunData();
    }
    return rundata;
  }

  protected JetspeedUser getUserFromContext() {
    JetspeedRunData rundata = getRunData();
    JetspeedUser user = null;
    if (rundata != null) {
      user = (JetspeedUser) rundata.getUser();
    }
    return user;
  }

  protected JetspeedRunData putUserIntoContext(JetspeedUser user) {
    JetspeedRunData rundata = getRunData();
    if (rundata != null) {

      rundata.setUser(user);
      rundata.save();
    }
    return rundata;
  }
}
