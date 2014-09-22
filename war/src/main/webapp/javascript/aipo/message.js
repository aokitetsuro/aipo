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

dojo.provide("aipo.message");

aipo.message.currentRoomId = null;
aipo.message.currentUserId = null;
aipo.message.currentGroupName = "all";
aipo.message.currentRoomSearchKeyword = null;
aipo.message.currentUserSearchKeyword = null;
aipo.message.moreMessageLock = false;

aipo.message.init = function() {
    aipo.message.reloadRoomList();
}

aipo.message.messagePane = null;
aipo.message.reloadMessageList = function() {
    if (!aipo.message.messagePane) {
        aipo.message.messagePane = dijit.byId("messagePane");
        aipo.message.messagePane = new aimluck.widget.Contentpane({},
                'messagePane');
        aipo.message.messagePane.onLoad = function() {
            aipo.message.read(aipo.message.currentRoomId);
        }
    }

    dojo.byId("messagePane").innerHTML = '<div class="loader"><i class="indicator"></i></div>';
    var screen = "?template=MessageListScreen";
    if (aipo.message.currentRoomId) {
        screen += "&r=" + aipo.message.currentRoomId;
    } else if (aipo.message.currentUserId) {
        screen += "&u=" + aipo.message.currentUserId;
    }
    aipo.message.messagePane.viewPage(screen);
}

aipo.message.moreMessageList = function(cursor) {
    aipo.message.moreMessageLock = true;
    var screen = "?template=MessageListScreen";
    if (aipo.message.currentRoomId) {
        screen += "&r=" + aipo.message.currentRoomId;
    } else if (aipo.message.currentUserId) {
        screen += "&u=" + aipo.message.currentUserId;
    }
    var messageLastMessageIdValues = dojo.query(".messageLastMessageIdValue");
    var cursor = messageLastMessageIdValues[messageLastMessageIdValues.length - 1].innerHTML;
    if (cursor) {
        screen += "&c=" + cursor;
        dojo.xhrGet({
            url : screen,
            timeout : 30000,
            encoding : "utf-8",
            handleAs : "text",
            headers : {
                X_REQUESTED_WITH : "XMLHttpRequest"
            },
            load : function(response, ioArgs) {
                messagePane.innerHTML += response;
                aipo.message.moreMessageLock = false;
            },
            error : function(error) {
                aipo.message.moreMessageLock = false;
            }
        });
    }
}

aipo.message.messageRoomListPane = null;
aipo.message.reloadRoomList = function(roomId) {
    if (!aipo.message.messageRoomListPane) {
        aipo.message.messageRoomListPane = dijit.byId("messageRoomListPane");
        aipo.message.messageRoomListPane = new aimluck.widget.Contentpane({},
                'messageRoomListPane');
        aipo.message.messageRoomListPane.onLoad = function() {
            var messageTotalUnreadCountValue = dojo
                    .byId("messageTotalUnreadCountValue");
            var count = parseInt(messageTotalUnreadCountValue.innerHTML);
            if (count != NaN) {
                aipo.menu.message.count(count);
            }
            if (aipo.message.messageRoomListPane.roomId) {
                aipo.message
                        .selectRoom(aipo.message.messageRoomListPane.roomId);
                aipo.message.messageRoomListPane.roomId = null;
            }
            if (aipo.message.currentUserId) {
                var messageCurrentRoomValue = dojo
                        .byId("messageCurrentRoomValue");
                var currentRoomId = parseInt(messageCurrentRoomValue.innerHTML);
                if (currentRoomId != NaN) {
                    aipo.message.selectRoom(currentRoomId);
                } else {
                    aipo.message.selectRoom(0);
                }
            }
        }
    }

    if (roomId) {
        aipo.message.messageRoomListPane.roomId = roomId;
    }

    var screen = "?template=MessageRoomListScreen";
    if (aipo.message.currentRoomId) {
        screen += "&r=" + aipo.message.currentRoomId;
    } else if (aipo.message.currentUserId) {
        screen += "&u=" + aipo.message.currentUserId;
    }
    if (aipo.message.currentRoomSearchKeyword) {
        screen += "&k="
                + encodeURIComponent(aipo.message.currentRoomSearchKeyword);
    }
    aipo.message.messageRoomListPane.viewPage(screen);
}
aipo.message.searchRoomList = function(form) {
    aipo.message.currentRoomSearchKeyword = form.keyword.value;
    aipo.message.reloadRoomList();
}

aipo.message.messageUserListPane = null;
aipo.message.reloadUserList = function(group_name) {
    if (!aipo.message.messageUserListPane) {
        aipo.message.messageUserListPane = dijit.byId("messageUserListPane");
        aipo.message.messageUserListPane = new aimluck.widget.Contentpane({},
                'messageUserListPane');
        aipo.message.messageUserListPane.onLoad = function() {
        }
    }

    if (group_name) {
        aipo.message.currentGroupName = group_name;
    }

    var screen = "?template=MessageUserListScreen&target_group_name="
            + aipo.message.currentGroupName;
    if (aipo.message.currentUserSearchKeyword) {
        screen += "&k="
                + encodeURIComponent(aipo.message.currentUserSearchKeyword);
    }

    aipo.message.messageUserListPane.viewPage(screen);
}

aipo.message.searchUserList = function() {
    var messageUserGroupSelect = dojo.byId("messageUserGroupSelect");
    var messageUserSearchForm = dojo.byId("messageUserSearchForm");
    aipo.message.currentUserSearchKeyword = messageUserSearchForm.keyword.value;
    aipo.message
            .reloadUserList(messageUserGroupSelect.options[messageUserGroupSelect.selectedIndex].value);
}

aipo.message.swapView = function() {
    if (dojo.byId("portletsBody") && dojo.byId("dd_message")) {
        if (dojo.hasClass("dd_message", "open")) {
            dojo.byId("portletsBody").style.display = "none";
            aipo.message.fixMessageWindow();
        } else {
            dojo.byId("portletsBody").style.display = "";
        }
    }
}

aipo.message.selectTab = function(tab) {
    var messageRoomTab = dojo.byId("messageRoomTab");
    var messageUserTab = dojo.byId("messageUserTab");
    var messageRoomContents = dojo.byId("messageRoomContents");
    var messageUserContents = dojo.byId("messageUserContents");

    if ("room" == tab) {
        dojo.addClass(messageRoomTab, "active");
        dojo.removeClass(messageUserTab, "active");
        dojo.addClass(messageRoomContents, "active");
        dojo.removeClass(messageUserContents, "active");
    }

    if ("user" == tab) {
        dojo.addClass(messageUserTab, "active");
        dojo.removeClass(messageRoomTab, "active");
        dojo.addClass(messageUserContents, "active");
        dojo.removeClass(messageRoomContents, "active");

        aipo.message.reloadUserList();
    }
}

aipo.message.inputHistory = {};
aipo.message.selectRoom = function(room_id) {
    var messageMainBlock = dojo.byId("messageMainBlock");
    var messageMainBlockEmpty = dojo.byId("messageMainBlockEmpty");
    var messageForm = dojo.byId("messageForm");
    var messageRoom = dojo.byId("messageRoom" + room_id);
    var messageRoomType = dojo.byId("messageRoomType" + room_id);
    var messageRoomAvatar = dojo.byId("messageRoomAvatar");
    var messageRoomName = dojo.byId("messageRoomName");
    var messageRoomSetting = dojo.byId("messageRoomSetting");
    if (messageForm && messageRoom) {
        messageMainBlock.style.display = "";
        messageMainBlockEmpty.style.display = "none";
        aipo.message.inputHistory[aipo.message.currentRoomId] = messageForm.message.value;
        aipo.message.currentRoomId = room_id;
        dojo.query(".messageSummary li").forEach(function(item) {
            dojo.removeClass(item, "active")
        });
        dojo.addClass(messageRoom, "active");
        if (aipo.message.inputHistory[aipo.message.currentRoomId]) {
            aipo.message
                    .changeInput(aipo.message.inputHistory[aipo.message.currentRoomId]);
        } else {
            aipo.message.clearInput();
        }
        messageRoomAvatar.innerHTML = dojo.query("#messageRoom" + room_id
                + " .avatar")[0].innerHTML;
        messageRoomName.innerHTML = dojo.query("#messageRoom" + room_id
                + " .name")[0].innerHTML;

        messageRoomSetting.style.display = "G" == messageRoomType.innerHTML ? ""
                : "none";

        if (room_id == 0 && aipo.message.currentUserId) {
            messageForm.roomId.value = 0;
            messageForm.userId.value = aipo.message.currentUserId;
        } else {
            messageForm.userId.value = 0;
            messageForm.roomId.value = aipo.message.currentRoomId;
        }

        aipo.message.reloadMessageList();
    }
}

aipo.message.selectUser = function(user_id) {
    var messageForm = dojo.byId("messageForm");
    if (messageForm) {
        aipo.message.currentRoomId = 0;
        aipo.message.currentUserId = user_id;
        aipo.message.inputHistory[aipo.message.currentRoomId] = messageForm.message.value;
        aipo.message.inputHistory[0] = "";
        aipo.message.reloadRoomList();
        aipo.message.selectTab("room");
    }
}

aipo.message.changeInput = function(value) {
    var messageForm = dojo.byId("messageForm");
    if (messageForm) {
        messageForm.message.value = value;
        aipo.message.resizeInput(messageForm.message);
        aipo.message.focusInput();
    }
}

aipo.message.clearInput = function() {
    var messageForm = dojo.byId("messageForm");
    if (messageForm) {
        messageForm.message.value = "";
        aipo.message.resizeInput(messageForm.message);
        aipo.message.focusInput();
    }
}

aipo.message.focusInput = function() {
    var messageForm = dojo.byId("messageForm");
    if (messageForm) {
        messageForm.message.focus();
    }
}

aipo.message.fixHeight = -31;
aipo.message.fixMessageWindow = function() {
    if (dojo.byId("dd_message") != null) {
        var minusH = 55 + 40 + 45;
        var w = document.documentElement.clientWidth - 20;
        var h = document.documentElement.clientHeight - minusH
                + aipo.message.fixHeight;
        var tabh = document.documentElement.clientHeight - minusH;
        dojo.byId("dd_message").style.width = w + "px";
        if (dojo.byId("messageSideBlock") != null) {
            dojo.byId("messageSideBlock").style.height = h + "px";
        }
        if (dojo.byId("messageSideBlock1") != null) {
            dojo.byId("messageSideBlock1").style.height = h + "px";
        }
        if (dojo.byId("messageSideBlock2") != null) {
            dojo.byId("messageSideBlock2").style.height = h + "px";
        }
    }
    if (dojo.byId("messagePane") != null) {
        var minusH = 55 + 40 + 145;
        var h = document.documentElement.clientHeight - minusH
                + aipo.message.fixHeight;
        dojo.byId("messagePane").style.height = h + "px";
    }
};

aipo.message.onLoadMessageRoomDialog = function() {
    var mpicker = dijit.byId("membernormalselect");
    if (mpicker) {
        var select = dojo.byId('init_memberlist');
        var i;
        var s_o = select.options;
        if (s_o.length == 1 && s_o[0].value == "")
            return;
        for (i = 0; i < s_o.length; i++) {
            mpicker.addOptionSync(s_o[i].value, s_o[i].text, true);
        }
    }
    var btn_ma = dojo.byId("button_member_add");
    if (btn_ma) {
        dojo.connect(btn_ma, "onclick", function() {
            aipo.message.expandMember();
        });
    }

    var btn_mr = dojo.byId("button_member_remove");
    if (btn_mr) {
        dojo.connect(btn_mr, "onclick", function() {
            aipo.message.expandMember();
        });
    }
    aipo.message.shrinkMember();
};

aipo.message.shrinkMember = function() {
    var node = dojo.byId("memberFieldButton");
    if (node) {
        var HTML = "";
        HTML += "<table class=\"w100\"><tbody><tr><td style=\"width:80%; border:none;\">";
        var m_t = dojo.byId("member_to");
        if (m_t) {
            var t_o = m_t.options;
            to_size = t_o.length;
            for (i = 0; i < to_size; i++) {
                var text = t_o[i].text.replace(/&/g, "&amp;").replace(/"/g,
                        "&quot;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
                HTML += "<span>" + text + "</span>";
                if (i < to_size - 1) {
                    HTML += ",<wbr/>";
                }
            }
        }
        HTML += "</td><td style=\"border:none;\">";
        HTML += '<input type=\"button\" class=\"alignright\" value=\"'
                + aimluck.io.escapeText("message_val_member1")
                + '\" onclick=\"aipo.message.expandMember();\" />'
        HTML += "</td></tr></tbody></table>";
        node.innerHTML = HTML;
    }

    var _node = dojo.byId("memberField");
    if (_node) {
        dojo.style(_node, "display", "none")
    }
    aipo.message.setWrapperHeight();
}

aipo.message.expandMember = function() {
    var node = dojo.byId("memberFieldButton");
    if (node) {
        var HTML = "";
        HTML += "<table class=\"w100\"><tbody><tr><td style=\"width:80%; border:none\">";
        var m_t = dojo.byId("member_to");
        if (m_t) {
            var t_o = m_t.options;
            to_size = t_o.length;
            for (i = 0; i < to_size; i++) {
                var text = t_o[i].text.replace(/&/g, "&amp;").replace(/"/g,
                        "&quot;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
                HTML += "<span>" + text + "</span>";
                if (i < to_size - 1) {
                    HTML += ",<wbr/>";
                }
            }
        }
        HTML += "</td><td style=\"border:none;\">";
        HTML += '<input type=\"button\" class=\"alignright\" value=\"'
                + aimluck.io.escapeText("message_val_member2")
                + '\" onclick=\"aipo.message.shrinkMember();\" />'
        HTML += "</td></tr></tbody></table>";
        node.innerHTML = HTML;
    }

    var _node = dojo.byId("memberField");
    if (_node) {
        dojo.style(_node, "display", "block");
    }
    aipo.message.setWrapperHeight();
}

aipo.message.onReceiveMessage = function(msg) {
    if (!msg) {
        aimluck.io.disableForm(dojo.byId("messageForm"), false);
        aipo.message.reloadMessageList();
        aipo.message.reloadRoomList();
        aipo.message.clearInput();
    }
};

aipo.message.onReceiveMessageRoom = function(msg) {
    if (!msg["error"]) {
        var arrDialog = dijit.byId("modalDialog");
        if (arrDialog) {
            arrDialog.hide();
            aipo.message.currentUserId = null;
            var tmpRoomId = parseInt(msg["params"]);
            if (tmpRoomId != NaN) {
                aipo.message.reloadRoomList(tmpRoomId);
            } else {
                aipo.message.reloadRoomList();
            }
        }
    }
    if (dojo.byId('messageDiv')) {
        dojo.byId('messageDiv').innerHTML = msg["error"];
    }
};

aipo.message.setWrapperHeight = function() {
    var modalDialog = document.getElementById('modalDialog');
    if (modalDialog) {
        var wrapper = document.getElementById('wrapper');
        wrapper.style.minHeight = modalDialog.clientHeight + 'px';
    }
}

aipo.message.resizeInput = function(input) {
    var shadowVal = input.value.replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/&/g, '&amp;').replace(/\n$/, '<br/>&nbsp;').replace(
                    /\n/g, '<br/>').replace(/ {2,}/g, function(space) {
                var result = "";
                var i = 0;
                while (i < space.length) {
                    result += '&nbsp;';
                    i++;
                }
                return result;
            });

    var shadowDiv = document.createElement("div");
    shadowDiv.id = "shadow-message"
    shadowDiv.style.position = "absolute";
    shadowDiv.style.top = "-1000";
    shadowDiv.style.left = "-1000";
    shadowDiv.style.border = "0";
    shadowDiv.style.outline = "0";
    shadowDiv.style.lineHeight = "normal";
    shadowDiv.style.height = "auto";
    shadowDiv.style.resize = "none";
    shadowDiv.cols = "10"
    shadowDiv.innerHTML = shadowVal + " ";

    var objBody = document.getElementsByTagName("body").item(0);

    objBody.appendChild(shadowDiv);
    var objShadow = dojo.byId("shadow-message");
    objShadow.style.width = input.offsetWidth + "px";

    var shadowHeight = objShadow.offsetHeight;

    if (shadowHeight < 18) {
        shadowHeight = 18;
    }
    input.style.height = shadowHeight * 1.2 + 21 + "px";
    objBody.removeChild(shadowDiv);
}

aipo.message.onPaste = function(input) {
    setTimeout(function() {
        aipo.message.resizeInput(input);
    }, 100);
}

aipo.message.read = function(room_id) {
    var messageRoomUnreadCount = dojo.byId("messageRoomUnreadCount" + room_id);
    if (messageRoomUnreadCount) {
        messageRoomUnreadCount.remove();
    }
    aipo.message.refreshUnreadCount();
}
aipo.message.refreshUnreadCount = function() {
    var total = 0;
    dojo.query(".messageSummary .nrCount").forEach(function(item) {
        var value = parseInt(item.innerHTML);
        if (value != NaN) {
            total += value;
        }
    });
    aipo.menu.message.count(total);
}

aipo.message.onFocusPlaceholder = function(input) {
    input.nextSibling.style.display = "none";
}

aipo.message.onBlurPlaceholder = function(input) {
    if (!input.value) {
        input.nextSibling.style.display = "";
    }
}

dojo.addOnLoad(function() {
    dojo.connect(window, "onresize", null, function(e) {
        aipo.message.fixMessageWindow();
    });
    var messagePane = dojo.byId("messagePane");
    if (messagePane) {
        dojo
                .connect(messagePane, "onscroll", null,
                        function(e) {
                            if (e.target.scrollTop + messagePane.clientHeight
                                    + 100 >= e.target.scrollHeight
                                    && !aipo.message.moreMessageLock) {
                                aipo.message.moreMessageList();
                            }
                        });
    }
});