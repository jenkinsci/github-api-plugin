package jenkins.plugins.github.api.mock;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import hudson.Util;
import java.io.Closeable;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.servlet.ServletException;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.jvnet.hudson.test.ThreadPoolImpl;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class MockGitHub implements Closeable {
    private AtomicLong nextId = new AtomicLong();
    private Map<String, MockUser> users = new HashMap<>();
    private Map<String, MockOrganization> organizations = new HashMap<>();

    private Server server;

    private int localPort = -1;
    private JsonFactory factory = new JsonFactory();

    public String open() throws IOException {
        server = new Server(new ThreadPoolImpl(
                new ThreadPoolExecutor(10, 10, 10L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
                        new ThreadFactory() {
                            public Thread newThread(Runnable r) {
                                Thread t = new Thread(r);
                                t.setName("Jetty Thread Pool");
                                return t;
                            }
                        })));
        ServletContextHandler context = new ServletContextHandler();
        server.setHandler(context);
        ;
        context.addServlet(Stapler.class, "/*");

        ServerConnector connector = new ServerConnector(server);
        HttpConfiguration config = connector.getConnectionFactory(HttpConnectionFactory.class).getHttpConfiguration();
        // use a bigger buffer as Stapler traces can get pretty large on deeply nested URL
        config.setRequestHeaderSize(12 * 1024);
        connector.setHost("localhost");

        server.addConnector(connector);
        try {
            server.start();
        } catch (IOException | RuntimeException | Error e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }

        localPort = connector.getLocalPort();
        context.getServletContext().setAttribute("app", this);
        return getUrl();
    }

    public String getUrl() {
        if (localPort == -1) {
            throw new IllegalStateException("Not open");
        }
        try {
            return new URI("http", null, "localhost", localPort, null, null, null).toASCIIString();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Will never happen", e);
        }
    }

    @Override
    public void close() throws IOException {
        localPort = -1;
        try {
            server.stop();
        } catch (IOException | RuntimeException | Error e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public Map<String, MockUser> getUsers() {
        return users;
    }

    public Map<String, MockOrganization> getOrgs() {
        return organizations;
    }

    public long nextId() {
        return nextId.incrementAndGet();
    }

    public String tz(long time) {
        SimpleDateFormat format = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss'Z'");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date(time));
    }

    public String pathSegment(String segment) {
        return Util.rawEncode(segment);
    }

    public String pathFragment(String fragment) {
        StringBuilder result = new StringBuilder(fragment.length() + 10);
        int i0 = 0;
        int i1 = fragment.indexOf('/');
        while (i0 != -1) {
            if (i0 > 0) {
                result.append('/');
            }
            if (i1 == -1) {
                result.append(pathSegment(fragment.substring(i0)));
                i0 = i1;
            } else {
                result.append(pathSegment(fragment.substring(i0, i1)));
                i0 = i1 + 1;
                i1 = fragment.indexOf('/', i0);
            }
        }
        return result.toString();
    }

    public String json(Object object) throws IOException {
        if (object == null) {
            return "null";
        }
        StringWriter w = new StringWriter();
        JsonGenerator generator = factory.createGenerator(w);
        generator.writeObject(object);
        generator.close();
        return w.toString();
    }

    public MockUser withUser(String login) {
        MockUser result = new MockUser(this, login);
        users.put(login, result);
        return result;
    }

    public MockOrganization withOrg(String login) {
        MockOrganization result = new MockOrganization(this, login);
        organizations.put(login, result);
        return result;
    }

    public List<MockOwner<?>> owners() {
        List<MockOwner<?>> result = new ArrayList<>(organizations.size() + users.size());
        result.addAll(users.values());
        result.addAll(organizations.values());
        return result;
    }

    public HttpResponse doRepositories(final @QueryParameter long since) {
        return new HttpResponse() {
            @Override
            public void generateResponse(StaplerRequest req, StaplerResponse rsp, Object node)
                    throws IOException, ServletException {
                List<MockRepository> repositories = new ArrayList<>();
                for (MockOwner<?> o : owners()) {
                    for (MockRepository r: o.repositories().values()) {
                        if (r.getId() > since && !r.isPrivate()) {
                            repositories.add(r);
                        }
                    }
                }
                Collections.sort(repositories, new Comparator<MockRepository>() {
                    @Override
                    public int compare(MockRepository o1, MockRepository o2) {
                        return Long.compare(o1.getId(), o2.getId());
                    }
                });
                if (repositories.size() > 30) {
                    rsp.addHeader("Link", String.format(
                            "<%s/repositories?since=%d>; rel=\"next\", <%s/repositories{?since}>; rel=\"first\"",
                            getUrl(), repositories.get(30).getId(), getUrl()));
                } else {
                    rsp.addHeader("Link", String.format("<%s/repositories{?since}>; rel=\"first\"", getUrl()));
                }
                rsp.setContentType("application/json; charset=utf-8");
                JsonGenerator o = factory.createGenerator(rsp.getOutputStream());
                o.writeStartArray();
                try {
                    for (MockRepository r : repositories.subList(0, Math.min(30, repositories.size()))) {
                        o.writeStartObject();
                        o.writeObjectField("id", r.getId());
                        o.writeObjectField("name", r.getName());
                        o.writeObjectField("full_name", r.owner().getLogin()+"/"+r.getName());
                        o.writeFieldName("owner");
                        o.writeStartObject();
                        o.writeObjectField("login", r.owner().getLogin());
                        o.writeObjectField("id", r.owner().getId());
                        o.writeObjectField("avatar_url", r.owner().getAvatarUrl());
                        o.writeObjectField("type", r.owner().getType());
                        o.writeEndObject();
                        o.writeObjectField("private", r.isPrivate());
                        o.writeObjectField("html_url", "https://github.com/"+r.owner().getLogin()+"/"+r.getName());

                        o.writeEndObject();
                    }
                } finally {
                    o.writeEndArray();
                    o.close();
                }

            }
        };
    }
}
