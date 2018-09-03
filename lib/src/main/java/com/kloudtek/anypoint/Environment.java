package com.kloudtek.anypoint;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.kloudtek.anypoint.api.API;
import com.kloudtek.anypoint.api.APIAsset;
import com.kloudtek.anypoint.api.APIList;
import com.kloudtek.anypoint.api.APISpec;
import com.kloudtek.anypoint.runtime.Server;
import com.kloudtek.anypoint.runtime.ServerGroup;
import com.kloudtek.util.StringUtils;
import com.kloudtek.util.validation.ValidationUtils;
import org.apache.http.client.methods.HttpRequestBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.kloudtek.util.StringUtils.isBlank;
import static com.kloudtek.util.StringUtils.isEmpty;

public class Environment extends AnypointObject<Organization> {
    private static final Logger logger = LoggerFactory.getLogger(Environment.class);
    private String id;
    private String name;
    private boolean production;
    private String type;
    private String clientId;

    public Environment() {
    }

    public Environment(Organization organization) {
        super(organization);
    }

    public Environment(Organization organization, String id) {
        super(organization);
        this.id = id;
    }

    @JsonIgnore
    public Organization getOrganization() {
        return parent;
    }

    @JsonProperty
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("isProduction")
    public boolean isProduction() {
        return production;
    }

    public void setProduction(boolean production) {
        this.production = production;
    }

    @JsonProperty
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @JsonProperty
    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @NotNull
    public String getServerRegistrationKey() throws HttpException {
        String json = httpHelper.httpGet("/hybrid/api/v1/servers/registrationToken", this);
        return (String) jsonHelper.toJsonMap(json).get("data");
    }

    public List<Server> getServers() throws HttpException {
        String json = client.getHttpHelper().httpGet("/armui/api/v1/servers", this);
        ArrayList<Server> list = new ArrayList<>();
        for (JsonNode node : jsonHelper.readJsonTree(json).at("/data")) {
            JsonNode type = node.get("type");
            Server s;
            if (type.asText().equals("SERVER_GROUP")) {
                s = jsonHelper.readJson(new ServerGroup(this), node);
            } else {
                s = jsonHelper.readJson(new Server(this), node);
            }
            list.add(s);
        }
        return list;
    }

    public ServerGroup createServerGroup(String name, String... serverIds) throws HttpException {
        if (serverIds == null) {
            serverIds = new String[0];
        }
        Map<String, Object> request = new HashMap<>();
        request.put("name", name);
        request.put("serverIds", serverIds);
        String json = httpHelper.httpPost("/hybrid/api/v1/serverGroups", request, this);
        return jsonHelper.readJson(new ServerGroup(this), json, "/data");
    }

    @NotNull
    public Server findServer(@NotNull String name) throws NotFoundException, HttpException {
        for (Server server : getServers()) {
            if (name.equals(server.getName())) {
                return server;
            }
        }
        throw new NotFoundException("Cannot find server : " + name);
    }

    public void addHeaders(HttpRequestBase method) {
        method.setHeader("X-ANYPNT-ORG-ID", parent.getId());
        method.setHeader("X-ANYPNT-ENV-ID", id);
    }

    public void delete() throws HttpException {
        for (Server server : getServers()) {
            server.delete();
        }
        httpHelper.httpDelete("/accounts/api/organizations/" + parent.getId() + "/environments/" + id);
        logger.info("Deleted environment " + id + " : " + name);
    }

    public Environment rename(String newName) throws HttpException {
        HashMap<String, String> req = new HashMap<>();
        req.put("id", id);
        req.put("name", newName);
        req.put("organizationId", parent.getId());
        String json = httpHelper.httpPut("/accounts/api/organizations/" + parent.getId() + "/environments/" + id, req);
        return jsonHelper.readJson(new Environment(parent), json);
    }

    @Override
    public String toString() {
        return "Environment{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", production=" + production +
                ", type='" + type + '\'' +
                ", clientId='" + clientId + '\'' +
                "} " + super.toString();
    }

    public APIList findAPIs() throws HttpException {
        return findAPIs(null);
    }

    public APIList findAPIs(String filter) throws HttpException {
        return new APIList(this, filter);
    }

    public API findAPIByExchangeAssetNameAndVersion(@NotNull String name, @NotNull String version) throws HttpException, NotFoundException {
        return findAPIByExchangeAssetNameAndVersion(name, version, null);
    }

    public API findAPIByExchangeAssetNameAndVersion(@NotNull String name, @NotNull String version, @Nullable String label) throws HttpException, NotFoundException {
        for (APIAsset asset : findAPIs(name)) {
            if (asset.getExchangeAssetName().equalsIgnoreCase(name)) {
                for (API api : asset.getApis()) {
                    if (api.getAssetVersion().equalsIgnoreCase(version) && (label == null || label.equalsIgnoreCase(api.getInstanceLabel()))) {
                        return api;
                    }
                }
            }
        }
        throw new NotFoundException("API " + name + " " + version + " not found");
    }

    public API findAPIByExchangeAsset(@NotNull String groupId, @NotNull String assetId, @NotNull String assetVersion) throws HttpException, NotFoundException {
        return findAPIByExchangeAsset(groupId, assetId, assetVersion, null);
    }

    public API findAPIByExchangeAsset(@NotNull String groupId, @NotNull String assetId, @NotNull String assetVersion, @Nullable String label) throws HttpException, NotFoundException {
        if(isBlank(groupId)) {
            throw new IllegalArgumentException("groupId missing (null or blank)");
        }
        if(isBlank(assetId)) {
            throw new IllegalArgumentException("assetId missing (null or blank)");
        }
        if(isBlank(assetVersion)) {
            throw new IllegalArgumentException("assetVersion missing (null or blank)");
        }
        for (APIAsset asset : findAPIs()) {
            if (asset.getGroupId().equalsIgnoreCase(groupId) && asset.getAssetId().equalsIgnoreCase(assetId) ) {
                for (API api : asset.getApis()) {
                    if (api.getAssetVersion().equalsIgnoreCase(assetVersion) && (label == null || label.equalsIgnoreCase(api.getInstanceLabel()))) {
                        return api;
                    }
                }
            }
        }
        throw new NotFoundException("API based on exchange asset not found: groupId=" + groupId + ", assetId=" + assetId + ", assetVersion="+assetVersion+", label="+label);
    }

    public enum Type {
        DESIGN, SANDBOX, PRODUCTION
    }

    public API createAPI(@NotNull APISpec apiSpec, boolean mule4, @Nullable String endpointUrl, @Nullable String label) throws HttpException {
        return API.create(this, apiSpec, mule4, endpointUrl, label);
    }

    @SuppressWarnings("unchecked")
    public static List<Environment> getEnvironments(@NotNull AnypointClient client, @NotNull Organization organization) throws HttpException {
        String json = client.getHttpHelper().httpGet("/accounts/api/organizations/" + organization.getId() + "/environments");
        return client.getJsonHelper().readJsonList((Class<Environment>) organization.getEnvironmentClass(), json, organization, "/data");
    }

    @NotNull
    public static Environment getEnvironmentByName(@NotNull String name, @NotNull AnypointClient client, @NotNull Organization organization) throws HttpException, NotFoundException {
        for (Environment environment : getEnvironments(client, organization)) {
            if (name.equals(environment.getName())) {
                return environment;
            }
        }
        throw new NotFoundException("Environment not found: " + name);
    }

    public String getNameOrId() {
        return name != null ? "(name) "+name : "(id) "+id;
    }
}
