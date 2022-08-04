/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package hotdog;

import com.github.gumtreediff.actions.EditScript;
import hotdog.change.Analyze;
import hotdog.editScript.GumTree;
import hotdog.git.GitInformation;
import hotdog.io.CLI;
import hotdog.io.CSV;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class pydiff {
    private String workPath;

    public static void main(String[] args) {
        new pydiff().run(args);
    }

    public void run(String[] args) {
        CLI cli = new CLI(args);
        workPath = cli.getWorkPath();

        if (cli.multiplePairs) {
            runMulitplePairs(cli.getCsvPath());
        }
        else if (cli.singlePair) {
            runSinglePair(cli.getSinglePairInfo());
        }

        int cnt=0;
        File file = new File("/home/nayeawon/keras-test.txt");
        for (String key : changeVectorPool.keySet()) {
            for (String pairInfo : changeVectorPool.get(key)) {
                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
                    writer.write(pairInfo + "\n");
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            cnt++;
            if (cnt < 30) break;
        }
        /*
        cli.parse option
        GitInformation . get Changed File list according to the commit
        File.parse
        diff (AST1, AST2)
        String editscript = analyze. changeAnalyze()
        String diff = diff().toString()
         */
    }

    private void runMulitplePairs(String csvPath) {
        ArrayList<String[]> csvContents = new CSV().readContents(csvPath);
        for (String[] content : csvContents) {
            runSinglePair(content);
        }
    }

    private HashMap<String, String[]> changeVectorPool = new HashMap<>();

    private void runSinglePair(String[] singlePairInfo) {
        String[] fileSources = new GitInformation().collect(workPath, singlePairInfo);
        EditScript editScript = new GumTree().generateEditScript(fileSources);
        if (editScript == null) return;
//        System.out.println(editScript.asList());
        String changeVector = new Analyze().generateChangeVector(editScript);
        String[] pairInfo = new String[]{singlePairInfo[1], singlePairInfo[2], singlePairInfo[3]};
        changeVectorPool.put(computeSHA256Hash(changeVector), pairInfo);
    }

    public String computeSHA256Hash(String hashString) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
            md.update(hashString.getBytes());
            byte bytes[] = md.digest();
            StringBuffer sb = new StringBuffer();
            for(byte b : bytes){
                sb.append(Integer.toString((b&0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
}
