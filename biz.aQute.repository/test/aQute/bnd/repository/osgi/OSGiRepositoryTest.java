package aQute.bnd.repository.osgi;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import aQute.bnd.build.Workspace;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.RepositoryListenerPlugin;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.progress.ProgressPlugin;
import aQute.bnd.version.Version;
import aQute.http.testservers.HttpTestServer.Config;
import aQute.lib.io.IO;
import aQute.libg.reporter.slf4j.Slf4jReporter;
import aQute.maven.provider.FakeNexus;
import junit.framework.TestCase;

public class OSGiRepositoryTest extends TestCase {
	File				tmp		= IO.getFile("generated/tmp");
	File				cache	= IO.getFile(tmp, "cache");
	File				remote	= IO.getFile(tmp, "testdata");
	File				ws		= IO.getFile(tmp, "ws");
	private FakeNexus	fnx;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		IO.delete(tmp);
		Config config = new Config();
		fnx = new FakeNexus(config, remote);
		fnx.start();
		remote.mkdirs();
		IO.copy(IO.getFile("testdata/minir5.xml"), IO.getFile(remote, "minir5.xml"));
		IO.copy(IO.getFile("testdata/bundles"), IO.getFile(remote, "bundles"));
	}

	public void testSimple() throws Exception {
		try (OSGiRepository r = new OSGiRepository();) {
			OSGiRepository.POLL_TIME = TimeUnit.MINUTES.toMillis(1);
			Map<String,String> map = new HashMap<>();
			map.put("locations", fnx.getBaseURI("/repo/minir5.xml").toString());
			map.put("cache", "generated/tmp/cache");
			map.put("max.stale", "10000");
			r.setProperties(map);
			Processor p = new Processor();
			HttpClient httpClient = new HttpClient();
			httpClient.setCache(cache);
			httpClient.setRegistry(p);
			p.addBasicPlugin(httpClient);
			p.setBase(ws);
			p.addBasicPlugin(Workspace.createStandaloneWorkspace(p, ws.toURI()));
			r.setRegistry(p);
			r.setReporter(new Slf4jReporter());

			final AtomicInteger tasks = new AtomicInteger();

			p.addBasicPlugin(new ProgressPlugin() {

				@Override
				public Task startTask(final String name, int size) {
					System.out.println("Starting " + name);
					tasks.incrementAndGet();
					return new Task() {

						@Override
						public void worked(int units) {
							System.out.println("Worked " + name + " " + units);
						}

						@Override
						public void done(String message, Throwable e) {
							System.out.println("Done " + name + " " + message);
						}

						@Override
						public boolean isCanceled() {
							return false;
						}
					};
				}
			});

			assertEquals(0, tasks.get());
			File file = r.get("dummybundle", new Version("0"), null);
			assertNotNull(file);
			assertEquals(2, tasks.get()); // 2 = index + file
			File file2 = r.get("dummybundle", new Version("0"), null);
			assertNotNull(file2);
			// second one should not have downloaded
			assertEquals(2, tasks.get());

			r.refresh();
			File file3 = r.get("dummybundle", new Version("0"), null);
			assertNotNull(file3);
			// second one should not have downloaded
			assertEquals(2, tasks.get());
		}
	}

	public void testPolling() throws Exception {
		try (OSGiRepository r = new OSGiRepository();) {
			OSGiRepository.POLL_TIME = TimeUnit.MILLISECONDS.toMillis(1);
			Map<String,String> map = new HashMap<>();
			map.put("locations", fnx.getBaseURI("/repo/minir5.xml").toString());
			map.put("cache", "generated/tmp/cache");
			map.put("max.stale", "10000");
			map.put("name", "test");
			r.setProperties(map);
			Processor p = new Processor();
			HttpClient httpClient = new HttpClient();
			httpClient.setCache(cache);
			httpClient.setRegistry(p);
			p.addBasicPlugin(httpClient);
			p.setBase(ws);
			p.addBasicPlugin(Workspace.createStandaloneWorkspace(p, ws.toURI()));
			r.setRegistry(p);
			r.setReporter(new Slf4jReporter());
			final AtomicReference<RepositoryPlugin> refreshed = new AtomicReference<>();
			p.addBasicPlugin(new RepositoryListenerPlugin() {

				@Override
				public void repositoryRefreshed(RepositoryPlugin repository) {
					refreshed.set(repository);
				}

				@Override
				public void repositoriesRefreshed() {
					// TODO Auto-generated method stub

				}

				@Override
				public void bundleRemoved(RepositoryPlugin repository, Jar jar, File file) {
					// TODO Auto-generated method stub

				}

				@Override
				public void bundleAdded(RepositoryPlugin repository, Jar jar, File file) {
					// TODO Auto-generated method stub

				}
			});
			File file = r.get("dummybundle", new Version("0"), null);
			assertNotNull(file);
			assertNull(r.title(new Object[0])); // not stale, default name

			// update the index file
			File index = IO.getFile(remote, "minir5.xml");
			String s = IO.collect(index);
			s += " "; // change the sha
			IO.store(s, index);
			Thread.sleep(100); // give the poller a chance

			assertEquals(r, refreshed.get());
			assertEquals("test [stale]", r.title(new Object[0]));
			System.out.println(r.tooltip(new Object[0]));
		}
	}

	public void testBndRepo() throws Exception {
		try (OSGiRepository r = new OSGiRepository();) {
			OSGiRepository.POLL_TIME = TimeUnit.MINUTES.toMillis(1);
			Map<String,String> map = new HashMap<>();
			map.put("locations",
					"https://bndtools.ci.cloudbees.com/job/bnd.master/lastSuccessfulBuild/artifact/dist/bundles/index.xml.gz");
			map.put("cache", "generated/tmp/cache");
			map.put("max.stale", "10000");
			r.setProperties(map);
			Processor p = new Processor();
			HttpClient httpClient = new HttpClient();
			httpClient.setCache(cache);
			httpClient.setRegistry(p);
			p.addBasicPlugin(httpClient);
			p.setBase(ws);
			p.addBasicPlugin(Workspace.createStandaloneWorkspace(p, ws.toURI()));
			r.setRegistry(p);
			r.setReporter(new Slf4jReporter());

			final AtomicInteger tasks = new AtomicInteger();

			p.addBasicPlugin(new ProgressPlugin() {

				@Override
				public Task startTask(final String name, int size) {
					System.out.println("Starting " + name);
					tasks.incrementAndGet();
					return new Task() {

						@Override
						public void worked(int units) {
							System.out.println("Worked " + name + " " + units);
						}

						@Override
						public void done(String message, Throwable e) {
							System.out.println("Done " + name + " " + message);
						}

						@Override
						public boolean isCanceled() {
							return false;
						}
					};
				}
			});

			assertEquals(0, tasks.get());
			List<String> list = r.list(null);
			assertFalse(list.isEmpty());

			SortedSet<Version> versions = r.versions("aQute.libg");
			assertFalse(versions.isEmpty());
			File f1 = r.get("aQute.libg", versions.first(), null);
			assertNotNull(f1);
			assertEquals(2, tasks.get()); // index + bundle

			File f2 = r.get("aQute.libg", versions.first(), null);
			assertEquals(2, tasks.get());// should use cache

			r.getIndex(true);
			File f3 = r.get("aQute.libg", versions.first(), null);
			assertEquals(4, tasks.get()); // should fetch again

		}
	}

}