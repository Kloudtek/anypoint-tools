package com.kloudtek.anypoint;

import com.kloudtek.util.UserDisplayableException;
import org.apache.maven.plugins.annotations.Parameter;

public abstract class AbstractEnvironmentalMojo extends AbstractOrganizationalMojo {
    /**
     * Anypoint Environment name
     */
    @Parameter(name = "env", property = "anypoint.env", required = true)
    protected String envName;

    @Override
    public void execute(AnypointClient client, Organization organization) throws Exception {
        try {
            Environment env = organization.findEnvironmentByName(envName);
            execute(client, env);
        } catch (NotFoundException e) {
            throw new UserDisplayableException("Unable to find environment " + envName + " in org " + organization.getName());
        }
    }

    public abstract void execute(AnypointClient client, Environment env) throws Exception;
}