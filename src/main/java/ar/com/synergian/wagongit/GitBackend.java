package ar.com.synergian.wagongit;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.log.ScmLogger;
import org.apache.maven.scm.provider.git.gitexe.command.GitCommandLineUtils;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.CommandLineUtils.StringStreamConsumer;
import org.codehaus.plexus.util.cli.Commandline;

public class GitBackend {

	private final File workDir;
	private final String remote;
	private final String branch;
	private boolean dirty = false;

	private final StringStreamConsumer stdout = new StringStreamConsumer();
	private final StringStreamConsumer stderr = new StringStreamConsumer();
	private final ScmLogger log;

	public GitBackend(File workDir, String url, ScmLogger log) {
		this.log = log;
		this.workDir = workDir;

		url = url.substring("git:".length());
		int i = url.indexOf(':');
		if (i < 0) {
			remote = url;
			branch = "master";
		} else {
			branch = url.substring(0, i);
			remote = url.substring(i + 3, url.length());
		}

		// TODO validate branch characters and strip remote.

	}

	private boolean run(String command) throws GitException {
		return run(command, null);
	}

	private boolean run(String command, String[] args) throws GitException {

		try {

			Commandline cl = GitCommandLineUtils.getBaseGitCommandLine(workDir,
					command);

			if (args != null) {
				for (int i = 0; i < args.length; i++)
					cl.createArgument().setValue(args[i]);
			}

			StringBuffer sb = new StringBuffer();
			sb.append("git ").append(command);
			if (args != null) {
				for (int i = 0; i < args.length; i++)
					sb.append(" ").append(args[i]);
			}

			int exitCode = GitCommandLineUtils.execute(cl, stdout, stderr, log);

			log.info("RAN: " + sb.toString() + " / $? = " + exitCode);

			return exitCode == 0;
		} catch (ScmException e) {
			throw new GitException("Couldn't run command " + command + ": "
					+ e.getMessage(), e);
		}
	}

	private boolean isValidRepo() {

		// TODO is this correct?
		// Where are assuming that this was checked out by this wagon.
		return new File(workDir, ".git").exists();
	}

	public void pullAll() throws GitException {

		// if there a valid ".git" directory?
		if (!isValidRepo()) {

			if (!run("init"))
				throw new GitException("git init failed");

			if (!run("remote", new String[] { "add", "origin", remote }))
				throw new GitException("git remote failed");

			if (!run("fetch"))
				throw new GitException("git fetch failed");
		}

		// if remote branch doesn't exist, create new "headless".
		if (!run("show-ref", new String[] { "refs/remotes/origin/" + branch })) {

			// git symbolic-ref HEAD refs/heads/<branch>
			if (!run("symbolic-ref", new String[] { "HEAD",
					"refs/heads/" + branch }))
				throw new GitException("Unable to create branch");

			// rm .git/index
			File index = new File(workDir, ".git/index");
			if (index.exists())
				if (!index.delete())
					throw new GitException("Unable to create branch");

			// git clean -fdx

			if (!run("clean", new String[] { "-fdx" }))
				throw new GitException("Unable to create branch");
		} else
		// else if local branch doesn't exist, checkout -b
		if (!run("show-ref", new String[] { "refs/heads/" + branch })) {

			// git checkout -b <branch> origin/<branch>
			if (!run("checkout", new String[] { "-b", branch,
					"origin/" + branch }))
				throw new GitException("Unable to checkout branch");

		}
		// else checkout local branch.
		else {
			// git checkout <branch>
			if (!run("checkout", new String[] { branch }))
				throw new GitException("Unable to checkout branch");
		}

	}

	public void pushAll() throws GitException {

		// There are some pull/push that doesn't add anything...
		if (dirty) {

			if (!run("add", new String[] { "." }))
				throw new GitException("Unable to add files");

			if (!run("commit", new String[] {
					"-m",
					"[wagon-git]" + " commit to branch " + branch + " "
							+ new Date().toGMTString() }))
				throw new GitException("Unable to commit files");

			if (!run("push", new String[] { "origin", branch }))
				throw new GitException("Unable to push files");
		}

		dirty = false;
	}

	public void put(File source, String destination) throws IOException {
		FileUtils.copyFile(source, new File(workDir, destination));
		dirty = true;
	}

	public void get(Resource resource, File localFile) throws IOException {

		File remote = new File(workDir, resource.getName());

		if (remote.exists())
			FileUtils.copyFile(remote, localFile);
	}

	public void putDirectory(File sourceDirectory, String destinationDirectory)
			throws IOException {

		FileUtils.copyDirectoryStructure(sourceDirectory, new File(workDir,
				destinationDirectory));

		dirty = true;
	}
}
