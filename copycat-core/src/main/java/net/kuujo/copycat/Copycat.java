/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.kuujo.copycat;

import net.kuujo.copycat.cluster.Cluster;
import net.kuujo.copycat.event.*;
import net.kuujo.copycat.log.Log;
import net.kuujo.copycat.spi.CopycatFactory;
import net.kuujo.copycat.spi.CorrelationStrategy;
import net.kuujo.copycat.spi.QuorumStrategy;
import net.kuujo.copycat.spi.TimerStrategy;
import net.kuujo.copycat.spi.protocol.CopycatProtocol;
import net.kuujo.copycat.spi.service.CopycatService;

import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;

/**
 * Copycat service.<p>
 *
 * This is the primary type for implementing full remote services on top of Copycat. A {@code Copycat} instance consists
 * of a {@link net.kuujo.copycat.CopycatContext} which controls logging and replication and a
 * {@link net.kuujo.copycat.spi.service.CopycatService} which exposes an endpoint through which commands can be
 * submitted to the Copycat cluster.<p>
 *
 * The {@code Copycat} constructor requires a {@link net.kuujo.copycat.CopycatContext} and
 * {@link net.kuujo.copycat.spi.service.CopycatService}:<p>
 *
 * {@code
 * StateMachine stateMachine = new MyStateMachine();
 * Log log = new MemoryMappedFileLog("data.log");
 * ClusterConfig<Member> config = new LocalClusterConfig();
 * config.setLocalMember("foo");
 * config.setRemoteMembers("bar", "baz");
 * Cluster<Member> cluster = new LocalCluster(config);
 * CopycatContext context = CopycatContext.context(stateMachine, log, cluster);
 *
 * CopycatService service = new HttpService("localhost", 8080);
 *
 * Copycat copycat = Copycat.copycat(service, context);
 * copycat.start();
 * }
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
public interface Copycat {
  static final CopycatFactory factory = ServiceLoader.load(CopycatFactory.class).iterator().next();

  /**
   * Returns a new copycat builder.
   *
   * @return A new copycat builder.
   */
  static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a new Copycat instance.
   *
   * @param service The Copycat service.
   * @param context The Copycat context.
   * @return A new Copycat instance.
   */
  static Copycat copycat(CopycatService service, CopycatContext context) {
    return factory.createCopycat(service, context);
  }

  /**
   * Creates a new Copycat instance.
   *
   * @param service The Copycat service.
   * @param stateMachine The Copycat state machine.
   * @param log The Copycat log.
   * @param cluster The Copycat cluster.
   * @param config The Copycat configuration.
   * @return A new Copycat instance.
   */
  static Copycat copycat(CopycatService service, StateMachine stateMachine, Log log, Cluster<?> cluster, CopycatConfig config) {
    return factory.createCopycat(service, CopycatContext.context(stateMachine, log, cluster, config));
  }

  /**
   * Returns the copycat context.
   *
   * @return The underlying copycat context.
   */
  CopycatContext context();

  /**
   * Returns the context events.
   *
   * @return Context events.
   */
  Events on();

  /**
   * Returns the context for a specific event.
   *
   * @param event The event for which to return the context.
   * @return The event context.
   */
  <T extends Event> EventContext<T> on(Class<T> event);

  /**
   * Returns the event handlers registry.
   *
   * @return The event handlers registry.
   */
  EventHandlers events();

  /**
   * Returns an event handler registry for a specific event.
   *
   * @param event The event for which to return the registry.
   * @return An event handler registry.
   */
  <T extends Event> EventHandlerRegistry<T> event(Class<T> event);

  /**
   * Starts the replica.
   *
   * @return A completable future to be completed once the replica has started.
   */
  CompletableFuture<Void> start();

  /**
   * Stops the replica.
   *
   * @return A completable future to be completed once the replica has stopped.
   */
  CompletableFuture<Void> stop();

  /**
   * Copycat builder.
   */
  public static class Builder {
    private CopycatService service;
    private final CopycatContext.Builder builder = CopycatContext.builder();

    private Builder() {
    }

    /**
     * Sets the copycat service.
     *
     * @param service The copycat service.
     * @return The copycat builder.
     */
    public Builder withService(CopycatService service) {
      this.service = service;
      return this;
    }

    /**
     * Sets the copycat log.
     *
     * @param log The copycat log.
     * @return The copycat builder.
     */
    public Builder withLog(Log log) {
      builder.withLog(log);
      return this;
    }

    /**
     * Sets the copycat configuration.
     *
     * @param config The copycat configuration.
     * @return The copycat builder.
     */
    public Builder withConfig(CopycatConfig config) {
      builder.withConfig(config);
      return this;
    }

    /**
     * Sets the copycat election timeout.
     *
     * @param timeout The copycat election timeout.
     * @return The copycat builder.
     */
    public Builder withElectionTimeout(long timeout) {
      builder.withElectionTimeout(timeout);
      return this;
    }

    /**
     * Sets the copycat heartbeat interval.
     *
     * @param interval The copycat heartbeat interval.
     * @return The copycat builder.
     */
    public Builder withHeartbeatInterval(long interval) {
      builder.withHeartbeatInterval(interval);
      return this;
    }

    /**
     * Sets whether to require quorums during reads.
     *
     * @param requireQuorum Whether to require quorums during reads.
     * @return The copycat builder.
     */
    public Builder withRequireReadQuorum(boolean requireQuorum) {
      builder.withRequireReadQuorum(requireQuorum);
      return this;
    }

    /**
     * Sets the read quorum size.
     *
     * @param quorumSize The read quorum size.
     * @return The copycat builder.
     */
    public Builder withReadQuorumSize(int quorumSize) {
      builder.withReadQuorumSize(quorumSize);
      return this;
    }

    /**
     * Sets the read quorum strategy.
     *
     * @param quorumStrategy The read quorum strategy.
     * @return The copycat builder.
     */
    public Builder withReadQuorumStrategy(QuorumStrategy<?> quorumStrategy) {
      builder.withReadQuorumStrategy(quorumStrategy);
      return this;
    }

    /**
     * Sets whether to require quorums during writes.
     *
     * @param requireQuorum Whether to require quorums during writes.
     * @return The copycat builder.
     */
    public Builder withRequireWriteQuorum(boolean requireQuorum) {
      builder.withRequireWriteQuorum(requireQuorum);
      return this;
    }

    /**
     * Sets the write quorum size.
     *
     * @param quorumSize The write quorum size.
     * @return The copycat builder.
     */
    public Builder withWriteQuorumSize(int quorumSize) {
      builder.withWriteQuorumSize(quorumSize);
      return this;
    }

    /**
     * Sets the write quorum strategy.
     *
     * @param quorumStrategy The write quorum strategy.
     * @return The copycat builder.
     */
    public Builder withWriteQuorumStrategy(QuorumStrategy<?> quorumStrategy) {
      builder.withWriteQuorumStrategy(quorumStrategy);
      return this;
    }

    /**
     * Sets the max log size.
     *
     * @param maxSize The max log size.
     * @return The copycat builder.
     */
    public Builder withMaxLogSize(int maxSize) {
      builder.withMaxLogSize(maxSize);
      return this;
    }

    /**
     * Sets the correlation strategy.
     *
     * @param strategy The correlation strategy.
     * @return The copycat builder.
     */
    public Builder withCorrelationStrategy(CorrelationStrategy<?> strategy) {
      builder.withCorrelationStrategy(strategy);
      return this;
    }

    /**
     * Sets the timer strategy.
     *
     * @param strategy The timer strategy.
     * @return The copycat builder.
     */
    public Builder withTimerStrategy(TimerStrategy strategy) {
      builder.withTimerStrategy(strategy);
      return this;
    }

    /**
     * Sets the cluster protocol.
     *
     * @param protocol The cluster protocol.
     * @return The copycat builder.
     */
    public Builder withProtocol(CopycatProtocol<?> protocol) {
      builder.withProtocol(protocol);
      return this;
    }

    /**
     * Sets the copycat cluster.
     *
     * @param cluster The copycat cluster.
     * @return The copycat builder.
     */
    public Builder withCluster(Cluster<?> cluster) {
      builder.withCluster(cluster);
      return this;
    }

    /**
     * Sets the copycat state machine.
     *
     * @param stateMachine The state machine.
     * @return The copycat builder.
     */
    public Builder withStateMachine(StateMachine stateMachine) {
      builder.withStateMachine(stateMachine);
      return this;
    }

    /**
     * Builds the copycat instance.
     *
     * @return The copycat instance.
     */
    public Copycat build() {
      return copycat(service, builder.build());
    }

    @Override
    public String toString() {
      return getClass().getSimpleName();
    }

  }

}
