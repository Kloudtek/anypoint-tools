package com.kloudtek.anypoint.api.policy;

import com.kloudtek.anypoint.AnypointObject;
import com.kloudtek.anypoint.api.APIVersion;
import com.kloudtek.anypoint.util.JsonHelper;

import java.util.Map;

public abstract class Policy extends AnypointObject<APIVersion> {
    protected Integer id;
    protected String policyTemplateId;

    public Policy() {
    }

    public Policy(APIVersion parent) {
        super(parent);
    }

    public Policy(APIVersion parent, Map<String,Object> data) {
        super(parent);
        id = (Integer) data.get("id");
        policyTemplateId = (String) data.get("policyTemplateId");
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPolicyTemplateId() {
        return policyTemplateId;
    }

    public void setPolicyTemplateId(String policyTemplateId) {
        this.policyTemplateId = policyTemplateId;
    }

    public JsonHelper.MapBuilder toJson() {
        return jsonHelper.buildJsonMap().set("id",id).set("apiVersionId",parent.getId());
    }

    public static Policy parseJson( APIVersion apiVersion, Map<String,Object> data ) {
        String policyTemplateId = (String) data.get("policyTemplateId");
        if (policyTemplateId.equals("client-id-enforcement")) {
            return new ClientIdEnforcementPolicy(apiVersion, data);
        } else {
            return new CustomPolicy(apiVersion,data);
        }
    }
}
