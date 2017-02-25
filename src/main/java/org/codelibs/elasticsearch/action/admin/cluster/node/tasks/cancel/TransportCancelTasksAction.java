/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.codelibs.elasticsearch.action.admin.cluster.node.tasks.cancel;

import org.codelibs.elasticsearch.ResourceNotFoundException;
import org.codelibs.elasticsearch.action.ActionListener;
import org.codelibs.elasticsearch.action.FailedNodeException;
import org.codelibs.elasticsearch.action.TaskOperationFailure;
import org.codelibs.elasticsearch.action.support.ActionFilters;
import org.codelibs.elasticsearch.action.support.tasks.TransportTasksAction;
import org.codelibs.elasticsearch.cluster.ClusterState;
import org.codelibs.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.codelibs.elasticsearch.cluster.node.DiscoveryNode;
import org.codelibs.elasticsearch.cluster.service.ClusterService;
import org.codelibs.elasticsearch.common.inject.Inject;
import org.codelibs.elasticsearch.common.io.stream.StreamInput;
import org.codelibs.elasticsearch.common.io.stream.StreamOutput;
import org.codelibs.elasticsearch.common.settings.Settings;
import org.codelibs.elasticsearch.common.util.concurrent.AtomicArray;
import org.codelibs.elasticsearch.tasks.CancellableTask;
import org.codelibs.elasticsearch.tasks.TaskId;
import org.codelibs.elasticsearch.tasks.TaskInfo;
import org.codelibs.elasticsearch.threadpool.ThreadPool;
import org.codelibs.elasticsearch.transport.EmptyTransportResponseHandler;
import org.codelibs.elasticsearch.transport.TransportChannel;
import org.codelibs.elasticsearch.transport.TransportException;
import org.codelibs.elasticsearch.transport.TransportRequest;
import org.codelibs.elasticsearch.transport.TransportRequestHandler;
import org.codelibs.elasticsearch.transport.TransportResponse;
import org.codelibs.elasticsearch.transport.TransportService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Transport action that can be used to cancel currently running cancellable tasks.
 * <p>
 * For a task to be cancellable it has to return an instance of
 * {@link CancellableTask} from {@link TransportRequest#createTask(long, String, String, TaskId)}
 */
public class TransportCancelTasksAction extends TransportTasksAction<CancellableTask, CancelTasksRequest, CancelTasksResponse, TaskInfo> {

    public static final String BAN_PARENT_ACTION_NAME = "internal:admin/tasks/ban";

    @Inject
    public TransportCancelTasksAction(Settings settings, ThreadPool threadPool, ClusterService clusterService,
                                      TransportService transportService, ActionFilters actionFilters, IndexNameExpressionResolver
                                          indexNameExpressionResolver) {
        super(settings, CancelTasksAction.NAME, threadPool, clusterService, transportService, actionFilters,
            indexNameExpressionResolver, CancelTasksRequest::new, CancelTasksResponse::new,
            ThreadPool.Names.MANAGEMENT);
        transportService.registerRequestHandler(BAN_PARENT_ACTION_NAME, BanParentTaskRequest::new, ThreadPool.Names.SAME, new
            BanParentRequestHandler());
    }

    @Override
    protected CancelTasksResponse newResponse(CancelTasksRequest request, List<TaskInfo> tasks, List<TaskOperationFailure>
        taskOperationFailures, List<FailedNodeException> failedNodeExceptions) {
        return new CancelTasksResponse(tasks, taskOperationFailures, failedNodeExceptions);
    }

    @Override
    protected TaskInfo readTaskResponse(StreamInput in) throws IOException {
        return new TaskInfo(in);
    }

    protected void processTasks(CancelTasksRequest request, Consumer<CancellableTask> operation) {
        if (request.getTaskId().isSet()) {
            // we are only checking one task, we can optimize it
            CancellableTask task = taskManager.getCancellableTask(request.getTaskId().getId());
            if (task != null) {
                if (request.match(task)) {
                    operation.accept(task);
                } else {
                    throw new IllegalArgumentException("task [" + request.getTaskId() + "] doesn't support this operation");
                }
            } else {
                if (taskManager.getTask(request.getTaskId().getId()) != null) {
                    // The task exists, but doesn't support cancellation
                    throw new IllegalArgumentException("task [" + request.getTaskId() + "] doesn't support cancellation");
                } else {
                    throw new ResourceNotFoundException("task [{}] doesn't support cancellation", request.getTaskId());
                }
            }
        } else {
            for (CancellableTask task : taskManager.getCancellableTasks().values()) {
                if (request.match(task)) {
                    operation.accept(task);
                }
            }
        }
    }

    @Override
    protected synchronized void taskOperation(CancelTasksRequest request, CancellableTask cancellableTask,
            ActionListener<TaskInfo> listener) {
        final BanLock banLock = new BanLock(nodes -> removeBanOnNodes(cancellableTask, nodes));
        Set<String> childNodes = taskManager.cancel(cancellableTask, request.getReason(), banLock::onTaskFinished);
        if (childNodes != null) {
            if (childNodes.isEmpty()) {
                // The task has no child tasks, so we can return immediately
                logger.trace("cancelling task {} with no children", cancellableTask.getId());
                listener.onResponse(cancellableTask.taskInfo(clusterService.localNode().getId(), false));
            } else {
                // The task has some child tasks, we need to wait for until ban is set on all nodes
                logger.trace("cancelling task {} with children on nodes [{}]", cancellableTask.getId(), childNodes);
                String nodeId = clusterService.localNode().getId();
                AtomicInteger responses = new AtomicInteger(childNodes.size());
                List<Exception> failures = new ArrayList<>();
                setBanOnNodes(request.getReason(), cancellableTask, childNodes, new ActionListener<Void>() {
                    @Override
                    public void onResponse(Void aVoid) {
                        processResponse();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        synchronized (failures) {
                            failures.add(e);
                        }
                        processResponse();
                    }

                    private void processResponse() {
                        banLock.onBanSet();
                        if (responses.decrementAndGet() == 0) {
                            if (failures.isEmpty() == false) {
                                IllegalStateException exception = new IllegalStateException("failed to cancel children of the task [" +
                                    cancellableTask.getId() + "]");
                                failures.forEach(exception::addSuppressed);
                                listener.onFailure(exception);
                            } else {
                                listener.onResponse(cancellableTask.taskInfo(nodeId, false));
                            }
                        }
                    }
                });

            }
        } else {
            logger.trace("task {} is already cancelled", cancellableTask.getId());
            throw new IllegalStateException("task with id " + cancellableTask.getId() + " is already cancelled");
        }
    }

    @Override
    protected boolean accumulateExceptions() {
        return true;
    }

    private void setBanOnNodes(String reason, CancellableTask task, Set<String> nodes, ActionListener<Void> listener) {
        sendSetBanRequest(nodes,
            BanParentTaskRequest.createSetBanParentTaskRequest(new TaskId(clusterService.localNode().getId(), task.getId()), reason),
            listener);
    }

    private void removeBanOnNodes(CancellableTask task, Set<String> nodes) {
        sendRemoveBanRequest(nodes,
            BanParentTaskRequest.createRemoveBanParentTaskRequest(new TaskId(clusterService.localNode().getId(), task.getId())));
    }

    private void sendSetBanRequest(Set<String> nodes, BanParentTaskRequest request, ActionListener<Void> listener) {
        ClusterState clusterState = clusterService.state();
        for (String node : nodes) {
            DiscoveryNode discoveryNode = clusterState.getNodes().get(node);
            if (discoveryNode != null) {
                // Check if node still in the cluster
                logger.trace("Sending ban for tasks with the parent [{}] to the node [{}], ban [{}]", request.parentTaskId, node,
                    request.ban);
                transportService.sendRequest(discoveryNode, BAN_PARENT_ACTION_NAME, request,
                    new EmptyTransportResponseHandler(ThreadPool.Names.SAME) {
                        @Override
                        public void handleResponse(TransportResponse.Empty response) {
                            listener.onResponse(null);
                        }

                        @Override
                        public void handleException(TransportException exp) {
                            logger.warn("Cannot send ban for tasks with the parent [{}] to the node [{}]", request.parentTaskId, node);
                            listener.onFailure(exp);
                        }
                    });
            } else {
                listener.onResponse(null);
                logger.debug("Cannot send ban for tasks with the parent [{}] to the node [{}] - the node no longer in the cluster",
                    request.parentTaskId, node);
            }
        }
    }

    private void sendRemoveBanRequest(Set<String> nodes, BanParentTaskRequest request) {
        ClusterState clusterState = clusterService.state();
        for (String node : nodes) {
            DiscoveryNode discoveryNode = clusterState.getNodes().get(node);
            if (discoveryNode != null) {
                // Check if node still in the cluster
                logger.debug("Sending remove ban for tasks with the parent [{}] to the node [{}]", request.parentTaskId, node);
                transportService.sendRequest(discoveryNode, BAN_PARENT_ACTION_NAME, request, EmptyTransportResponseHandler
                    .INSTANCE_SAME);
            } else {
                logger.debug("Cannot send remove ban request for tasks with the parent [{}] to the node [{}] - the node no longer in " +
                    "the cluster", request.parentTaskId, node);
            }
        }
    }

    private static class BanLock {
        private final Consumer<Set<String>> finish;
        private final AtomicInteger counter;
        private final AtomicReference<Set<String>> nodes = new AtomicReference<>();

        public BanLock(Consumer<Set<String>> finish) {
            counter = new AtomicInteger(0);
            this.finish = finish;
        }

        public void onBanSet() {
            if (counter.decrementAndGet() == 0) {
                finish();
            }
        }

        public void onTaskFinished(Set<String> nodes) {
            this.nodes.set(nodes);
            if (counter.addAndGet(nodes.size()) == 0) {
                finish();
            }
        }

        public void finish() {
            finish.accept(nodes.get());
        }

    }

    private static class BanParentTaskRequest extends TransportRequest {

        private TaskId parentTaskId;

        private boolean ban;

        private String reason;

        static BanParentTaskRequest createSetBanParentTaskRequest(TaskId parentTaskId, String reason) {
            return new BanParentTaskRequest(parentTaskId, reason);
        }

        static BanParentTaskRequest createRemoveBanParentTaskRequest(TaskId parentTaskId) {
            return new BanParentTaskRequest(parentTaskId);
        }

        private BanParentTaskRequest(TaskId parentTaskId, String reason) {
            this.parentTaskId = parentTaskId;
            this.ban = true;
            this.reason = reason;
        }

        private BanParentTaskRequest(TaskId parentTaskId) {
            this.parentTaskId = parentTaskId;
            this.ban = false;
        }

        public BanParentTaskRequest() {
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            parentTaskId = TaskId.readFromStream(in);
            ban = in.readBoolean();
            if (ban) {
                reason = in.readString();
            }
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            parentTaskId.writeTo(out);
            out.writeBoolean(ban);
            if (ban) {
                out.writeString(reason);
            }
        }
    }

    class BanParentRequestHandler implements TransportRequestHandler<BanParentTaskRequest> {
        @Override
        public void messageReceived(final BanParentTaskRequest request, final TransportChannel channel) throws Exception {
            if (request.ban) {
                logger.debug("Received ban for the parent [{}] on the node [{}], reason: [{}]", request.parentTaskId,
                    clusterService.localNode().getId(), request.reason);
                taskManager.setBan(request.parentTaskId, request.reason);
            } else {
                logger.debug("Removing ban for the parent [{}] on the node [{}]", request.parentTaskId,
                    clusterService.localNode().getId());
                taskManager.removeBan(request.parentTaskId);
            }
            channel.sendResponse(TransportResponse.Empty.INSTANCE);
        }
    }


}
