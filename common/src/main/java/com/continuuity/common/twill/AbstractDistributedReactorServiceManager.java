package com.continuuity.common.twill;

import com.continuuity.common.conf.CConfiguration;
import com.continuuity.common.conf.Constants;
import com.google.common.base.Preconditions;
import org.apache.twill.api.TwillController;
import org.apache.twill.api.TwillRunnerService;
import org.apache.twill.discovery.Discoverable;
import org.apache.twill.discovery.DiscoveryServiceClient;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Abstract class that can be extended by individual Reactor Services to implement their management methods.
 */
public abstract class AbstractDistributedReactorServiceManager implements ReactorServiceManager {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractDistributedReactorServiceManager.class);
  private static final long SERVICE_PING_RESPONSE_TIMEOUT = TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS);
  protected final long discoveryTimeout;

  protected CConfiguration cConf;
  protected TwillRunnerService twillRunnerService;
  protected String serviceName;
  protected DiscoveryServiceClient discoveryServiceClient;

  public AbstractDistributedReactorServiceManager(CConfiguration cConf, String serviceName,
                                                  TwillRunnerService twillRunnerService,
                                                  DiscoveryServiceClient discoveryServiceClient) {
    this.cConf = cConf;
    this.serviceName = serviceName;
    this.twillRunnerService = twillRunnerService;
    this.discoveryTimeout = cConf.getLong(Constants.Monitor.DISCOVERY_TIMEOUT_SECONDS);
    this.discoveryServiceClient = discoveryServiceClient;
  }

  @Override
  public int getRequestedInstances() {
    //todo this will be updated
    Iterable<TwillController> twillControllerList = twillRunnerService.lookup(Constants.Service.REACTOR_SERVICES);
    int instances = 0;
    if (twillControllerList != null) {
      for (TwillController twillController : twillControllerList) {
        instances = twillController.getResourceReport().getRunnableResources(serviceName).size();
      }
    }
    return instances;
  }

  @Override
  public int getProvisionedInstances() {
    Iterable<TwillController> twillControllerList = twillRunnerService.lookup(Constants.Service.REACTOR_SERVICES);
    int instances = 0;
    if (twillControllerList != null) {
      for (TwillController twillController : twillControllerList) {
        instances = twillController.getResourceReport().getRunnableResources(serviceName).size();
      }
    }
    return instances;
  }

  @Override
  public boolean setInstances(int instanceCount) {
    Preconditions.checkArgument(instanceCount > 0);
    try {
      Iterable<TwillController> twillControllerList = twillRunnerService.lookup(Constants.Service.REACTOR_SERVICES);
      if (twillControllerList != null) {
        for (TwillController twillController : twillControllerList) {
          twillController.changeInstances(serviceName, instanceCount).get();
        }
      }
      return true;
    } catch (InterruptedException e) {
      e.printStackTrace();
      return false;
    } catch (ExecutionException e) {
      e.printStackTrace();
      return false;
    }
  }

  @Override
  public int getMinInstances() {
    return 1;
  }

  @Override
  public boolean canCheckStatus() {
    return true;
  }

  @Override
  public boolean isLogAvailable() {
    return true;
  }

  @Override
  public boolean isServiceAvailable() {
    try {
      Iterable<Discoverable> discoverables = this.discoveryServiceClient.discover(serviceName);
      for (Discoverable discoverable : discoverables) {
        //Ping the discovered service to check its status.
        String url = String.format("http://%s:%d/ping", discoverable.getSocketAddress().getHostName(),
                                   discoverable.getSocketAddress().getPort());
        if (checkGetStatus(url).equals(HttpResponseStatus.OK)) {
          return true;
        }
      }
      return false;
    } catch (IllegalArgumentException e) {
      return false;
    } catch (Exception e) {
      LOG.warn("Unable to ping {} : Reason : {}", serviceName, e.getMessage());
      return false;
    }
  }

  protected final HttpResponseStatus checkGetStatus(String url) throws Exception {
    HttpURLConnection httpConn = null;
    try {
      httpConn = (HttpURLConnection) new URL(url).openConnection();
      httpConn.setConnectTimeout((int) SERVICE_PING_RESPONSE_TIMEOUT);
      httpConn.setReadTimeout((int) SERVICE_PING_RESPONSE_TIMEOUT);
      return (HttpResponseStatus.valueOf(httpConn.getResponseCode()));
    } catch (SocketTimeoutException e) {
      return HttpResponseStatus.NOT_FOUND;
    } finally {
      if (httpConn != null) {
        httpConn.disconnect();
      }
    }
  }
}
