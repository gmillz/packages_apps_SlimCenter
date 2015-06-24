package com.slim.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Shell {

    public class CommandResult {
        public int exitValue;
        public String output;
        public String error;

        public CommandResult(int exit, String out, String er) {
            exitValue = exit;
            output = out;
            error = er;
        }

        public boolean success() {
            return exitValue == 0;
        }


    }

    public static final String SYSTEM_MOUNT = "/system";

    public SH sh;
    public SH su;

    public Shell() {
        sh = new SH("sh");
        su = new SH("su");
    }

    public class SH {

        private String SHELL = "sh";

        public SH(String shell) {
            SHELL = shell;
        }

        public boolean runCommand(String c) {
            Integer exitValue = 0;
            try {
                Process p = Runtime.getRuntime().exec(SHELL);
                DataOutputStream dos = new DataOutputStream(p.getOutputStream());
                dos.writeBytes(c);
                dos.writeBytes("exit\n");
                dos.flush();
                dos.close();
                exitValue = p.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return exitValue == 0;
        }

        public CommandResult run(String c) {
            Process p = null;
            int exitValue = 0;
            try {
                p = Runtime.getRuntime().exec(SHELL);
                DataOutputStream dos = new DataOutputStream(p.getOutputStream());
                dos.writeBytes(c);
                dos.writeBytes("exit\n");
                dos.flush();
                dos.close();
                exitValue = p.waitFor();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return new CommandResult(exitValue, getStreamLines(p.getInputStream()),
                    getStreamLines(p.getErrorStream()));
        }
    }

    private String getStreamLines(final InputStream is) {
        BufferedReader br;
        StringBuilder sb = new StringBuilder();
        try {
            br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public static void remountFileSystem(String fileSystem, String mount) {
        final Shell.SH cmd = new Shell().su;
        cmd.runCommand("mount -o remount," + mount + " " + fileSystem + "\n");
    }

}