package hotdog.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class GitInformation {

    private static DiffAlgorithm diffAlgorithm = DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.MYERS);
    private static RawTextComparator diffComparator = RawTextComparator.WS_IGNORE_ALL;

    public void collect(String workPath, String[] singlePairInfo) {
        String repoName = singlePairInfo[0];
        String cpc = singlePairInfo[1];
        String pc = singlePairInfo[2];
        String filePath = singlePairInfo[3];

        Repository repo = getRepo(workPath, repoName);

        RevCommit pcCommit = null, cpcCommit = null;
        try {
            pcCommit = getCommitById(repo, pc);
            cpcCommit = getCommitById(repo, cpc);
        } catch (IOException e) { e.printStackTrace(); }

        RevCommit parent = pcCommit.getParent(0);

        List<DiffEntry> diffs = diff(parent, pcCommit, repo);
        String srcFileSource = "", dstFileSource = "";
        for (DiffEntry diff : diffs) {
            if (diff.getNewPath() == filePath) {
                srcFileSource = getChangedFileContents(repo, pcCommit.getId().getName() + "~1", diff.getOldPath());
                dstFileSource = getChangedFileContents(repo, pcCommit.getId().getName(), diff.getNewPath());
                break;
            }
        }
        
    }

    public Repository getRepo(String workPath, String RepoName) {
        try {
            File file = new File(workPath + "/" + RepoName + ".git");

            Git git;
            if (file.exists()) git = Git.open(file);
            else throw new IOException("No cloned project in the given work path");

            return git.getRepository();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Nullable
    private RevCommit getCommitById(Repository repo, String id) throws IOException {
        ObjectId objectId = null;
        objectId = repo.resolve(id);
        return objectId == null ? null : new RevWalk(repo).parseCommit(objectId);
    }

    public static String getChangedFileContents(Repository repo, String revSpec, String path) {
        try {
            final ObjectId id = repo.resolve(revSpec);
            ObjectReader reader = repo.newObjectReader();
            RevWalk walk = new RevWalk(reader);
            RevCommit commit = walk.parseCommit(id);
            walk.close();

            RevTree tree = commit.getTree();
            TreeWalk treewalk = TreeWalk.forPath(reader, path, tree);
            if (treewalk != null) {
                byte[] data = reader.open(treewalk.getObjectId(0)).getBytes();
                reader.close();
                return new String(data, "utf-8");
            } else { return ""; }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static List<DiffEntry> diff(RevCommit parent, RevCommit commit, Repository repo) {
        DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
        df.setRepository(repo);
        df.setDiffAlgorithm(diffAlgorithm);
        df.setDiffComparator(diffComparator);
        df.setDetectRenames(true);
        List<DiffEntry> diffs = null;
        try {
            diffs = df.scan(parent.getTree(), commit.getTree());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return diffs;
    }
}
