package ar.com.synergian.wagongit;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.Random;

public final class Utils {

	private Utils() {
	}

	public static File createCheckoutDirectory() {
		File checkoutDirectory;

		DecimalFormat fmt = new DecimalFormat("#####");

		Random rand = new Random(System.currentTimeMillis() + Runtime.getRuntime().freeMemory());

		do {

			checkoutDirectory = new File(System.getProperty("java.io.tmpdir"), "wagon-git-" + fmt.format(Math.abs(rand.nextInt())));

		} while (checkoutDirectory.exists());

		return checkoutDirectory;
	}

	private static String sha1(String input) throws NoSuchAlgorithmException {

		MessageDigest mDigest = MessageDigest.getInstance("SHA1");
		byte[] result = mDigest.digest(input.getBytes());
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < result.length; i++)
			sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16).substring(1));

		return sb.toString();
	}

	/**
	 * Creates a directory under given workDir for each URL, using the URL.
	 * Normally a hash, but if unavailable, the URL encoding of the URL. We do
	 * this in order to support multiple repos corresponding to different
	 * remotes.
	 */
	public static String hashPath(String path) throws GitException {
		try {

			return sha1(path);

		} catch (NoSuchAlgorithmException e) {

			// Okay. Something more basic.
			try {

				return URLEncoder.encode(path, "UTF-8");

			} catch (UnsupportedEncodingException x) {
				throw new GitException("Unable to encode path", x);
			}
		}
	}

	public static boolean getBooleanEnvironmentProperty(String key) {

		return Boolean.parseBoolean(System.getProperty(key, "false"));

	}
}
