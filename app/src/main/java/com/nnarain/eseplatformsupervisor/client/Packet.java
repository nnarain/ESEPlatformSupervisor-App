package com.nnarain.eseplatformsupervisor.client;

import java.util.ArrayList;

/**
 * Created by Natesh on 12/04/2015.
 */
public class Packet {

    private String contents;

    private Packet(String str)
    {
        this.contents = str;
    }

    public enum Command
    {
        PING("P"),
        SYNC("Z");

        private String value;

        Command(String v)
        {
            value = v;
        }

        public String getValue() {
            return value;
        }
    }

    public class Builder
    {
        private String command;
        private ArrayList<String> arguments;

        public Builder()
        {
            this.arguments = new ArrayList<String>();
        }

        public Packet build()
        {
            StringBuilder builder = new StringBuilder();

            builder
                    .append("<")
                    .append(command);

            for(final String s : arguments)
            {
                builder
                        .append(" ")
                        .append(s);
            }

            builder.append(">");

            return new Packet(builder.toString());
        }

        public Packet.Builder setCommand(Command cmd)
        {
            this.command = cmd.getValue();
            return this;
        }
    }

}
