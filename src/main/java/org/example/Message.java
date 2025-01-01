package org.example;

import java.io.Serializable;

public class Message implements Serializable {
    private final String command;
    private final Object object;

    public Message(String command, Object object) {
        this.command = command;
        this.object = object;
    }

    public Message(String command) {
        this.command = command;
        this.object = null;
    }

    public String getCommand() {
        return command;
    }

    public Object getObject() {
        return object;
    }
}
