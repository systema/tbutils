package com.systema.eia.iot.tb.clients;

import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;

import java.util.Map;

import static org.springframework.util.StringUtils.isEmpty;

public class InternalRestClientHelpers {

    /**
     * Copy of ThingsBoard RestClient's private {@code getTimeUrlParams()} method to make this code accessible for
     * {@link ExtRestClient#getAlarms(EntityId, AlarmSearchStatus, AlarmStatus, TimePageLink, boolean)}.
     */
    public static String getTimeUrlParams(TimePageLink pageLink) {
        String urlParams = getUrlParams(pageLink);
        if (pageLink.getStartTime() != null) {
            urlParams += "&startTime={startTime}";
        }
        if (pageLink.getEndTime() != null) {
            urlParams += "&endTime={endTime}";
        }
        return urlParams;
    }

    public static String getUrlParams(PageLink pageLink) {
        String urlParams = "pageSize={pageSize}&page={page}";
        if (!isEmpty(pageLink.getTextSearch())) {
            urlParams += "&textSearch={textSearch}";
        }
        if (pageLink.getSortOrder() != null) {
            urlParams += "&sortProperty={sortProperty}&sortOrder={sortOrder}";
        }
        return urlParams;
    }

    /**
     * Copy of ThingsBoard RestClient's private {@code addTimePageLinkToParam()} method to make this code accessible for
     * {@link ExtRestClient#getAlarms(EntityId, AlarmSearchStatus, AlarmStatus, TimePageLink, boolean)}.
     */
    public static void addTimePageLinkToParam(Map<String, String> params, TimePageLink pageLink) {
        addPageLinkToParam(params, pageLink);
        if (pageLink.getStartTime() != null) {
            params.put("startTime", String.valueOf(pageLink.getStartTime()));
        }
        if (pageLink.getEndTime() != null) {
            params.put("endTime", String.valueOf(pageLink.getEndTime()));
        }
    }

    /**
     * Copy of ThingsBoard RestClient's private {@code addTimePageLinkToParam()} method to make this code accessible for
     * {@link #addTimePageLinkToParam(Map, TimePageLink)}.
     */
    public static void addPageLinkToParam(Map<String, String> params, PageLink pageLink) {
        params.put("pageSize", String.valueOf(pageLink.getPageSize()));
        params.put("page", String.valueOf(pageLink.getPage()));
        if (!isEmpty(pageLink.getTextSearch())) {
            params.put("textSearch", pageLink.getTextSearch());
        }
        if (pageLink.getSortOrder() != null) {
            params.put("sortProperty", pageLink.getSortOrder().getProperty());
            params.put("sortOrder", pageLink.getSortOrder().getDirection().name());
        }
    }

}
