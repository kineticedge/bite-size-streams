package io.kineticedge.kstutorial.common.config;

import com.beust.jcommander.Parameter;

public abstract class BaseOptions {

    @Parameter(names = "--help", help = true, hidden = true)
    private boolean help;

    @Parameter(names = { "-b", "--bootstrap-servers" }, description = "cluster bootstrap servers")
    private String bootstrapServers = "localhost:9092";

    public boolean isHelp() {
        return help;
    }

    public String bootstrapServers() {
        return bootstrapServers;
    }

}
