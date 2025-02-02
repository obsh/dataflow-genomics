package com.google.allenday.genomics.core.cmd;

import org.javatuples.Triplet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;

public class CmdExecutor implements Serializable {

    private Logger LOG = LoggerFactory.getLogger(CmdExecutor.class);

    public Triplet<Boolean, Integer, String> executeCommand(String cmdCommand) {
        return executeCommand(cmdCommand, true);
    }

    public Triplet<Boolean, Integer, String> executeCommand(String cmdCommand, boolean inheritIO) {
        LOG.info(String.format("Executing command: %s", cmdCommand));
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (inheritIO) {
            processBuilder.inheritIO();
        }
        processBuilder.command("bash", "-c", cmdCommand);

        StringBuilder response = new StringBuilder();
        try {

            Process process = processBuilder.start();
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                LOG.info(line);
                response.append(line);
            }

            int exitCode = process.waitFor();
            LOG.info("\nExited with error code : " + exitCode);
            return Triplet.with(exitCode == 0, exitCode, response.toString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return Triplet.with(false, -1, response.toString());
        }
    }

}
