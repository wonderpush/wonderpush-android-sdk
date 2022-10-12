package com.wonderpush.sdk;

import org.junit.Assert;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class JSONSyncTest {

    private JSONSyncMock sync;

    private static class JSONSyncMock extends JSONSync {
        private MockServer server;

        void setServer(MockServer server) {
            this.server = server;
        }

        @Override
        public void persist(JSONObject state) {}

        @Override
        public void schedulePatchCall() {}

        @Override
        public void serverPatchInstallation(JSONObject diff, JSONSync.ResponseHandler handler) {
            server.serverPatchInstallation(diff, handler);
        }

        @Override
        public void upgrade(JSONObject upgradeMeta, JSONObject sdkState, JSONObject serverState, JSONObject putAccumulator, JSONObject inflightDiff, JSONObject inflightPutAccumulator) {

        }
    }

    private static abstract class MockServer {

        private boolean called = false;

        boolean isCalled() {
            return called;
        }

        final void serverPatchInstallation(JSONObject diff, JSONSync.ResponseHandler handler) {
            Assert.assertFalse("Server mock object must not be reused", called);
            called = true;
            _serverPatchInstallation_diff(diff);
            try {
                _serverPatchInstallation_do();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            _serverPatchInstallation_handler(handler);
        }

        protected void _serverPatchInstallation_diff(JSONObject diff) {}
        protected void _serverPatchInstallation_do() throws Exception {}
        protected void _serverPatchInstallation_handler(JSONSync.ResponseHandler handler) {}
    }

    private static class ServerManualCall extends MockServer {
        private JSONSync.ResponseHandler handler;
        @Override
        protected void _serverPatchInstallation_handler(JSONSync.ResponseHandler handler) {
            this.handler = handler;
        }
        public void callHandler() {
            this.handler.onSuccess();
        }
    }

    private class ServerAssertNotCalled extends MockServer {

        @Override
        public void _serverPatchInstallation_diff(JSONObject diff) {
            Assert.fail("serverPatchInstallation should not be called\nGot diff: " + diff + "\nSync state: " + sync);
        }

    }

    private static class ServerAssertDiffAndSuccess extends MockServer {

        private final String message;
        private final JSONObject expectedDiff;

        ServerAssertDiffAndSuccess(final String message, final JSONObject expectedDiff) {
            this.message = message;
            this.expectedDiff = expectedDiff;
        }

        @Override
        public void _serverPatchInstallation_diff(JSONObject diff) {
            JSONUtilTest.assertEquals(message, expectedDiff, diff);
        }

        @Override
        public void _serverPatchInstallation_handler(JSONSync.ResponseHandler handler) {
            handler.onSuccess();
        }

    }

    private static class ServerAssertDiffAndFailure extends MockServer {
        private final String message;
        private final JSONObject expectedDiff;

        ServerAssertDiffAndFailure(String message, JSONObject expectedDiff) {
            this.message = message;
            this.expectedDiff = expectedDiff;
        }

        @Override
        public void _serverPatchInstallation_diff(JSONObject diff) {
            JSONUtilTest.assertEquals(message, expectedDiff, diff);
        }

        @Override
        public void _serverPatchInstallation_handler(JSONSync.ResponseHandler handler) {
            handler.onFailure();
        }

    }

    @Before
    public void setup() {
        sync = new JSONSyncMock();
    }

    private void assertSynced() {
        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertFalse(sync.hasScheduledPatchCall());
        MockServer server = new ServerAssertNotCalled();
        sync.setServer(server);
        Assert.assertFalse(sync.performScheduledPatchCall());
        Assert.assertFalse(server.isCalled());
    }

    private void assertSyncedPotentialNoopScheduledPatchCall() {
        assertPotentialNoopScheduledPatchCall();
        assertSynced();
    }

    private void assertPotentialNoopScheduledPatchCall() {
        if (sync.hasScheduledPatchCall()) {
            assertNoopScheduledPatchCall();
        } else {
            Assert.assertTrue(true);
        }
    }

    private void assertNoopScheduledPatchCall() {
        Assert.assertFalse(sync.hasInflightPatchCall());
        MockServer server = new ServerAssertNotCalled();
        sync.setServer(server);
        Assert.assertTrue(sync.performScheduledPatchCall());
        Assert.assertFalse(server.isCalled());
    }

    private void assertPerformScheduledPatchCallWith(MockServer server) {
        sync.setServer(server);
        Assert.assertTrue(sync.performScheduledPatchCall());
        Assert.assertTrue(server.isCalled());
    }

    /* ****************************************** *
     * Test put() behavior wrt server patch calls *
     * ****************************************** */

    @Test
    public void initialState() throws JSONException {
        JSONUtilTest.assertEquals(new JSONObject(), sync.getSdkState());
        assertSynced();
        JSONUtilTest.assertEquals(new JSONObject(), sync.getSdkState());
    }

    @Test
    public void singlePutNullObject() throws JSONException {
        sync.put(null);
        JSONUtilTest.assertEquals(new JSONObject("{}"), sync.getSdkState());
        assertSyncedPotentialNoopScheduledPatchCall();
    }

    @Test
    public void singlePutNullField() throws JSONException {
        sync.put(new JSONObject("{\"A\":null}"));
        JSONUtilTest.assertEquals(new JSONObject("{}"), sync.getSdkState());
        assertSyncedPotentialNoopScheduledPatchCall();
    }

    @Test
    public void singlePutSuccess() throws JSONException {
        sync.put(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"), sync.getSdkState());

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"), sync.getSdkState());

        assertSyncedPotentialNoopScheduledPatchCall();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"), sync.getSdkState());
    }

    @Test
    public void singlePutFailure() throws JSONException {
        sync.put(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"), sync.getSdkState());

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndFailure(null, new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"), sync.getSdkState());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"), sync.getSdkState());

        assertSyncedPotentialNoopScheduledPatchCall();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"), sync.getSdkState());
    }

    @Test
    public void subsequentSinglePutSuccess() throws JSONException {
        singlePutSuccess();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"), sync.getSdkState());
        sync.put(new JSONObject("{\"AA\":null,\"AAA\":2,\"B\":2,\"BB\":2}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AAA\":2,\"B\":2,\"BB\":2}"), sync.getSdkState());

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"AA\":null,\"AAA\":2,\"B\":2,\"BB\":2}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AAA\":2,\"B\":2,\"BB\":2}"), sync.getSdkState());

        assertSyncedPotentialNoopScheduledPatchCall();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AAA\":2,\"B\":2,\"BB\":2}"), sync.getSdkState());
    }

    @Test
    public void receiveStateKeepsDiffs() throws JSONException {
        ServerManualCall server = new ServerManualCall();
        sync.setServer(server);
        sync.put(new JSONObject("{\"A\":1}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1}"), sync.getSdkState());
        sync.performScheduledPatchCall();
        Assert.assertTrue(sync.hasInflightPatchCall());
        sync.put(new JSONObject("{\"A\":2}"));
        sync.receiveState(new JSONObject("{}"), false);
        server.callHandler();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":2}"), sync.getSdkState());
    }

    @Test
    public void subsequentSinglePutFailure() throws JSONException {
        singlePutSuccess();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"), sync.getSdkState());
        sync.put(new JSONObject("{\"AA\":null,\"AAA\":2,\"B\":2,\"BB\":2}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AAA\":2,\"B\":2,\"BB\":2}"), sync.getSdkState());

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndFailure(null, new JSONObject("{\"AA\":null,\"AAA\":2,\"B\":2,\"BB\":2}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AAA\":2,\"B\":2,\"BB\":2}"), sync.getSdkState());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"AA\":null,\"AAA\":2,\"B\":2,\"BB\":2}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AAA\":2,\"B\":2,\"BB\":2}"), sync.getSdkState());

        assertSyncedPotentialNoopScheduledPatchCall();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AAA\":2,\"B\":2,\"BB\":2}"), sync.getSdkState());
    }

    @Test
    public void pendingPutSuccess() throws JSONException {
        sync.put(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"), sync.getSdkState());

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        sync.put(new JSONObject("{\"AA\":null,\"AAA\":2,\"B\":2,\"BB\":2}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AAA\":2,\"B\":2,\"BB\":2}"), sync.getSdkState());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"A\":1,\"AAA\":2,\"B\":2,\"BB\":2}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AAA\":2,\"B\":2,\"BB\":2}"), sync.getSdkState());

        assertSyncedPotentialNoopScheduledPatchCall();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AAA\":2,\"B\":2,\"BB\":2}"), sync.getSdkState());
    }

    @Test
    public void pendingPutFailure() throws JSONException {
        sync.put(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"), sync.getSdkState());

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        sync.put(new JSONObject("{\"AA\":null,\"AAA\":2,\"B\":2,\"BB\":2}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AAA\":2,\"B\":2,\"BB\":2}"), sync.getSdkState());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndFailure(null, new JSONObject("{\"A\":1,\"AAA\":2,\"B\":2,\"BB\":2}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AAA\":2,\"B\":2,\"BB\":2}"), sync.getSdkState());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"A\":1,\"AAA\":2,\"B\":2,\"BB\":2}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AAA\":2,\"B\":2,\"BB\":2}"), sync.getSdkState());

        assertSyncedPotentialNoopScheduledPatchCall();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AAA\":2,\"B\":2,\"BB\":2}"), sync.getSdkState());
    }

    @Test
    public void putWhileInflightSuccess() throws JSONException {
        sync.put(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"), sync.getSdkState());

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}")) {
            @Override
            protected void _serverPatchInstallation_do() throws Exception {
                sync.put(new JSONObject("{\"AA\":null,\"AAA\":2,\"B\":2,\"BB\":2}"));
                JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AAA\":2,\"B\":2,\"BB\":2}"), sync.getSdkState());
            }
        });
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AAA\":2,\"B\":2,\"BB\":2}"), sync.getSdkState());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"AA\":null,\"AAA\":2,\"B\":2,\"BB\":2}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AAA\":2,\"B\":2,\"BB\":2}"), sync.getSdkState());

        assertSyncedPotentialNoopScheduledPatchCall();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AAA\":2,\"B\":2,\"BB\":2}"), sync.getSdkState());
    }

    @Test
    public void putWhileInflightFailure() throws JSONException {
        sync.put(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"), sync.getSdkState());

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndFailure(null, new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}")) {
            @Override
            protected void _serverPatchInstallation_do() throws Exception {
                sync.put(new JSONObject("{\"AA\":null,\"AAA\":2,\"B\":2,\"BB\":2}"));
                JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AAA\":2,\"B\":2,\"BB\":2}"), sync.getSdkState());
            }
        });
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AAA\":2,\"B\":2,\"BB\":2}"), sync.getSdkState());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"A\":1,\"AAA\":2,\"B\":2,\"BB\":2}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AAA\":2,\"B\":2,\"BB\":2}"), sync.getSdkState());

        assertSyncedPotentialNoopScheduledPatchCall();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AAA\":2,\"B\":2,\"BB\":2}"), sync.getSdkState());
    }

    /* ************************************************** *
     * Test receiveDiff() behavior wrt server patch calls *
     * ************************************************** */

    private void assertSyncedAfterRecvDiff() {
        // Be laxer with this final state check
        //assertNoopScheduledPatchCall();
        //assertSynced();
        assertSyncedPotentialNoopScheduledPatchCall();
    }

    @Test
    public void recvDiffNullObject() throws JSONException {
        assertSynced();
        JSONUtilTest.assertEquals(new JSONObject(), sync.getSdkState());
        sync.receiveDiff(null);
        JSONUtilTest.assertEquals(new JSONObject("{}"), sync.getSdkState());
        assertSyncedAfterRecvDiff();
    }

    @Test
    public void recvDiffFromInitialState() throws JSONException {
        assertSynced();
        JSONUtilTest.assertEquals(new JSONObject(), sync.getSdkState());
        sync.receiveDiff(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"), sync.getSdkState());
        assertSyncedAfterRecvDiff();
    }

    @Test
    public void recvDiffFromNonEmptyState() throws JSONException {
        singlePutSuccess();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"), sync.getSdkState());
        assertSynced();

        sync.receiveDiff(new JSONObject("{\"AA\":null,\"AAA\":2,\"B\":2,\"BB\":2}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AAA\":2,\"B\":2,\"BB\":2}"), sync.getSdkState());
        assertSyncedAfterRecvDiff();
    }

    @Test
    public void recvDiffPendingPutSuccess() throws JSONException {
        singlePutSuccess();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"), sync.getSdkState());
        assertSynced();

        sync.put(new JSONObject("{\"AA\":2,\"AAA\":null,\"B\":2,\"BB\":2}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":2,\"B\":2,\"BB\":2}"), sync.getSdkState());
        // Pending diff is: {"AA":2, "AAA":null, "B":2, "BB":2}

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        sync.receiveDiff(new JSONObject("{\"AAA\":3,\"BB\":3,\"C\":3}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":2,\"AAA\":3,\"B\":2,\"BB\":3,\"C\":3}"), sync.getSdkState());
        // Pending diff loses "AAA" and "BB" (common keys) when accepting this received diff

        // Hence pending diff is now: {"AA":2, "B":2}
        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"AA\":2,\"B\":2}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":2,\"AAA\":3,\"B\":2,\"BB\":3,\"C\":3}"), sync.getSdkState());

        assertSyncedPotentialNoopScheduledPatchCall();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":2,\"AAA\":3,\"B\":2,\"BB\":3,\"C\":3}"), sync.getSdkState());
    }

    @Test
    public void recvDiffPendingPutFailure() throws JSONException {
        singlePutSuccess();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"), sync.getSdkState());
        assertSynced();

        sync.put(new JSONObject("{\"AA\":2,\"AAA\":null,\"B\":2,\"BB\":2}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":2,\"B\":2,\"BB\":2}"), sync.getSdkState());
        // Pending diff is: {"AA":2, "AAA":null, "B":2, "BB":2}

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        sync.receiveDiff(new JSONObject("{\"AAA\":3,\"BB\":3,\"C\":3}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":2,\"AAA\":3,\"B\":2,\"BB\":3,\"C\":3}"), sync.getSdkState());
        // Pending diff loses "AAA" and "BB" (common keys) when accepting this received diff

        // Hence pending diff is now: {"AA":2, "B":2}
        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndFailure(null, new JSONObject("{\"AA\":2,\"B\":2}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":2,\"AAA\":3,\"B\":2,\"BB\":3,\"C\":3}"), sync.getSdkState());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"AA\":2,\"B\":2}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":2,\"AAA\":3,\"B\":2,\"BB\":3,\"C\":3}"), sync.getSdkState());

        assertSyncedPotentialNoopScheduledPatchCall();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":2,\"AAA\":3,\"B\":2,\"BB\":3,\"C\":3}"), sync.getSdkState());
    }

    @Test
    public void recvDiffInflightPutSuccess() throws JSONException {
        singlePutSuccess();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"), sync.getSdkState());
        assertSynced();

        sync.put(new JSONObject("{\"AA\":2,\"AAA\":null,\"B\":2,\"BB\":2}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":2,\"B\":2,\"BB\":2}"), sync.getSdkState());
        // Pending diff is: {"AA":2, "AAA":null, "B":2, "BB":2}

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"AA\":2,\"AAA\":null,\"B\":2,\"BB\":2}")) {
            @Override
            protected void _serverPatchInstallation_do() throws Exception {
                sync.receiveDiff(new JSONObject("{\"AAA\":3,\"BB\":3,\"C\":3}"));
                JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":2,\"AAA\":3,\"B\":2,\"BB\":3,\"C\":3}"), sync.getSdkState());
                // Pending diff becomes: {"AAA":3, "BB":3} (keys common to inflight diff and received diff with values from received diff)
                // instead of empty, because the inflight call has overwritten them
            }
        });
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":2,\"AAA\":3,\"B\":2,\"BB\":3,\"C\":3}"), sync.getSdkState());

        // Pending diff is: {"AAA":3, "BB":3} (see previous comment) because call succeeded
        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"AAA\":3,\"BB\":3}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":2,\"AAA\":3,\"B\":2,\"BB\":3,\"C\":3}"), sync.getSdkState());

        assertSyncedAfterRecvDiff();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":2,\"AAA\":3,\"B\":2,\"BB\":3,\"C\":3}"), sync.getSdkState());
    }

    @Test
    public void recvDiffInflightPutFailure() throws JSONException {
        singlePutSuccess();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"), sync.getSdkState());
        assertSynced();

        sync.put(new JSONObject("{\"AA\":2,\"AAA\":null,\"B\":2,\"BB\":2}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":2,\"B\":2,\"BB\":2}"), sync.getSdkState());
        // Pending diff is: {"AA":2, "AAA":null, "B":2, "BB":2}

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndFailure(null, new JSONObject("{\"AA\":2,\"AAA\":null,\"B\":2,\"BB\":2}")) {
            @Override
            protected void _serverPatchInstallation_do() throws Exception {
                sync.receiveDiff(new JSONObject("{\"AAA\":3,\"BB\":3,\"C\":3}"));
                JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":2,\"AAA\":3,\"B\":2,\"BB\":3,\"C\":3}"), sync.getSdkState());
                // Pending diff becomes: {"AAA":3, "BB":3} (keys common to inflight diff and received diff with values from received diff)
                // instead of empty, because the inflight call has overwritten them
                // BUT we will fail this call
            }
        });
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":2,\"AAA\":3,\"B\":2,\"BB\":3,\"C\":3}"), sync.getSdkState());
        // Pending diff is: {"AA":2, "B":2}, the previous pending diff minus received keys ("AAA" and "BB")

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"AA\":2,\"B\":2}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":2,\"AAA\":3,\"B\":2,\"BB\":3,\"C\":3}"), sync.getSdkState());

        assertSyncedPotentialNoopScheduledPatchCall();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":2,\"AAA\":3,\"B\":2,\"BB\":3,\"C\":3}"), sync.getSdkState());
    }

    /* ********************************************************** *
     * Test receiveState(, false) behavior wrt server patch calls *
     * ********************************************************** */

    private void assertSyncedAfterRecvState() {
        // Be laxer with this final state check
        //assertNoopScheduledPatchCall();
        //assertSynced();
        assertSyncedPotentialNoopScheduledPatchCall();
    }

    @Test
    public void rectStateNullObject() throws JSONException {
        assertSynced();
        JSONUtilTest.assertEquals(new JSONObject(), sync.getSdkState());
        sync.receiveState(null, false);
        JSONUtilTest.assertEquals(new JSONObject("{}"), sync.getSdkState());
        assertSyncedAfterRecvState();
    }

    @Test
    public void rectStateNullFieldFromInitialState() throws JSONException {
        assertSynced();
        JSONUtilTest.assertEquals(new JSONObject(), sync.getSdkState());
        sync.receiveState(new JSONObject("{\"A\":null}"), false);
        JSONUtilTest.assertEquals(new JSONObject("{}"), sync.getSdkState());
        assertSyncedAfterRecvState();
    }

    @Test
    public void rectStateFromInitialState() throws JSONException {
        assertSynced();
        JSONUtilTest.assertEquals(new JSONObject(), sync.getSdkState());
        sync.receiveState(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"), false);
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"), sync.getSdkState());
        assertSyncedAfterRecvState();
    }

    @Test
    public void recvStateFromNonEmptyState() throws JSONException {
        singlePutSuccess();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"), sync.getSdkState());
        assertSynced();

        sync.receiveState(new JSONObject("{\"A\":1,\"AAA\":2,\"B\":2,\"BB\":2}"), false);
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AAA\":2,\"B\":2,\"BB\":2}"), sync.getSdkState());
        assertSyncedAfterRecvState();
    }

    @Test
    public void recvStatePendingPutSuccess() throws JSONException {
        singlePutSuccess();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"), sync.getSdkState());
        assertSynced();

        sync.put(new JSONObject("{\"AA\":null,\"AAA\":null,\"B\":2,\"BB\":2,\"BBB\":2}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2,\"BB\":2,\"BBB\":2}"), sync.getSdkState());
        // Pending diff is: {"AA":null, "AAA":null, "B":2, "BB":2, "BBB":2}

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        sync.receiveState(new JSONObject("{\"AAA\":3,\"BBB\":3,\"C\":3}"), false);
        // "A"   is removed
        // "AA"  is removed even if part of the pending diff, because server state does not have it
        // "AAA" is not updated because it's part of the pending diff
        // "B"   is not removed because it's part of the pending diff
        // "BB"  is not removed because it's part of the pending diff
        // "BBB" is not updated because it's part of the pending diff
        // "C"   is added
        // Pending diff is unchanged because it has the priority for conflict resolution
        JSONUtilTest.assertEquals(new JSONObject("{\"B\":2,\"BB\":2,\"BBB\":2,\"C\":3}"), sync.getSdkState());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"AAA\":null,\"B\":2,\"BB\":2,\"BBB\":2}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"B\":2,\"BB\":2,\"BBB\":2,\"C\":3}"), sync.getSdkState());

        assertSyncedPotentialNoopScheduledPatchCall();
        JSONUtilTest.assertEquals(new JSONObject("{\"B\":2,\"BB\":2,\"BBB\":2,\"C\":3}"), sync.getSdkState());
    }

    @Test
    public void recvStatePendingPutFailure() throws JSONException {
        singlePutSuccess();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"), sync.getSdkState());
        assertSynced();

        sync.put(new JSONObject("{\"AA\":null,\"AAA\":null,\"B\":2,\"BB\":2,\"BBB\":2}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2,\"BB\":2,\"BBB\":2}"), sync.getSdkState());

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        sync.receiveState(new JSONObject("{\"AAA\":3,\"BBB\":3,\"C\":3}"), false);
        // "A"   is removed
        // "AA"  is removed even if part of the pending diff, because server state does not have it
        // "AAA" is not updated because it's part of the pending diff
        // "B"   is not removed because it's part of the pending diff
        // "BB"  is not removed because it's part of the pending diff
        // "BBB" is not updated because it's part of the pending diff
        // "C"   is added
        // Pending diff is unchanged because it has the priority for conflict resolution
        JSONUtilTest.assertEquals(new JSONObject("{\"B\":2,\"BB\":2,\"BBB\":2,\"C\":3}"), sync.getSdkState());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndFailure(null, new JSONObject("{\"AAA\":null,\"B\":2,\"BB\":2,\"BBB\":2}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"B\":2,\"BB\":2,\"BBB\":2,\"C\":3}"), sync.getSdkState());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"AAA\":null,\"B\":2,\"BB\":2,\"BBB\":2}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"B\":2,\"BB\":2,\"BBB\":2,\"C\":3}"), sync.getSdkState());

        assertSyncedPotentialNoopScheduledPatchCall();
        JSONUtilTest.assertEquals(new JSONObject("{\"B\":2,\"BB\":2,\"BBB\":2,\"C\":3}"), sync.getSdkState());
    }

    @Test
    public void recvStateInflightPutSuccess() throws JSONException {
        singlePutSuccess();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"), sync.getSdkState());
        assertSynced();

        sync.put(new JSONObject("{\"AA\":null,\"AAA\":null,\"B\":2,\"BB\":2,\"BBB\":2}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2,\"BB\":2,\"BBB\":2}"), sync.getSdkState());

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"AA\":null,\"AAA\":null,\"B\":2,\"BB\":2,\"BBB\":2}")) {
            @Override
            protected void _serverPatchInstallation_do() throws Exception {
                sync.receiveState(new JSONObject("{\"AAA\":3,\"BBB\":3,\"C\":3}"), false);
                // "A"   is removed
                // "AA"  is not removed because it's part of the inflight diff
                // "AAA" is not updated because it's part of the inflight diff
                // "B"   is not removed because it's part of the inflight diff
                // "BB"  is not removed because it's part of the inflight diff
                // "BBB" is not updated because it's part of the inflight diff
                // "C"   is added
                JSONUtilTest.assertEquals(new JSONObject("{\"B\":2,\"BB\":2,\"BBB\":2,\"C\":3}"), sync.getSdkState());
            }
        });
        JSONUtilTest.assertEquals(new JSONObject("{\"B\":2,\"BB\":2,\"BBB\":2,\"C\":3}"), sync.getSdkState());

        assertSyncedAfterRecvState();
        JSONUtilTest.assertEquals(new JSONObject("{\"B\":2,\"BB\":2,\"BBB\":2,\"C\":3}"), sync.getSdkState());
    }

    @Test
    public void recvStateInflightPutFailure() throws JSONException {
        singlePutSuccess();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"), sync.getSdkState());
        assertSynced();

        sync.put(new JSONObject("{\"AA\":null,\"AAA\":null,\"B\":2,\"BB\":2,\"BBB\":2}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2,\"BB\":2,\"BBB\":2}"), sync.getSdkState());

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndFailure(null, new JSONObject("{\"AA\":null,\"AAA\":null,\"B\":2,\"BB\":2,\"BBB\":2}")) {
            @Override
            protected void _serverPatchInstallation_do() throws Exception {
                sync.receiveState(new JSONObject("{\"AAA\":3,\"BBB\":3,\"C\":3}"), false);
                // "A"   is removed
                // "AA"  is removed even if part of the inflight diff, because server state does not have it
                // "AAA" is not updated because it's part of the inflight diff
                // "B"   is not removed because it's part of the inflight diff
                // "BB"  is not removed because it's part of the inflight diff
                // "BBB" is not updated because it's part of the inflight diff
                // "C"   is added
                // Pending diff is unchanged because it has the priority for conflict resolution
                JSONUtilTest.assertEquals(new JSONObject("{\"B\":2,\"BB\":2,\"BBB\":2,\"C\":3}"), sync.getSdkState());
            }
        });
        JSONUtilTest.assertEquals(new JSONObject("{\"B\":2,\"BB\":2,\"BBB\":2,\"C\":3}"), sync.getSdkState());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"AAA\":null,\"B\":2,\"BB\":2,\"BBB\":2}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"B\":2,\"BB\":2,\"BBB\":2,\"C\":3}"), sync.getSdkState());

        assertSyncedPotentialNoopScheduledPatchCall();
        JSONUtilTest.assertEquals(new JSONObject("{\"B\":2,\"BB\":2,\"BBB\":2,\"C\":3}"), sync.getSdkState());
    }

    /* ********************************************************* *
     * Test receiveState(, true) behavior wrt server patch calls *
     * ********************************************************* */

    private void assertSyncedAfterRecvStateReset() {
        // Be laxer with this final state check
        //assertNoopScheduledPatchCall();
        //assertSynced();
        assertSyncedPotentialNoopScheduledPatchCall();
    }

    @Test
    public void recvStateResetNullObject() throws JSONException {
        assertSynced();
        JSONUtilTest.assertEquals(new JSONObject(), sync.getSdkState());
        sync.receiveState(null, true);
        JSONUtilTest.assertEquals(new JSONObject("{}"), sync.getSdkState());
        assertSyncedAfterRecvStateReset();
    }

    @Test
    public void recvStateResetNullFieldFromInitialState() throws JSONException {
        assertSynced();
        JSONUtilTest.assertEquals(new JSONObject(), sync.getSdkState());
        sync.receiveState(new JSONObject("{\"A\":null}"), true);
        JSONUtilTest.assertEquals(new JSONObject("{}"), sync.getSdkState());
        assertSyncedAfterRecvStateReset();
    }

    @Test
    public void recvStateResetFromInitialState() throws JSONException {
        assertSynced();
        JSONUtilTest.assertEquals(new JSONObject(), sync.getSdkState());
        sync.receiveState(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"), true);
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"), sync.getSdkState());
        assertSyncedAfterRecvStateReset();
    }

    @Test
    public void recvStateResetFromNonEmptyState() throws JSONException {
        singlePutSuccess();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"), sync.getSdkState());
        assertSynced();

        sync.receiveState(new JSONObject("{\"A\":1,\"AAA\":2,\"B\":2,\"BB\":2}"), true);
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AAA\":2,\"B\":2,\"BB\":2}"), sync.getSdkState());
        assertSyncedAfterRecvStateReset();
    }

    @Test
    public void recvStateResetPendingPut() throws JSONException {
        singlePutSuccess();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"), sync.getSdkState());
        assertSynced();

        sync.put(new JSONObject("{\"AA\":2,\"AAA\":null,\"B\":2,\"BB\":2,\"BBB\":2}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":2,\"B\":2,\"BB\":2,\"BBB\":2}"), sync.getSdkState());
        // Pending diff is: {"AA":2, "AAA":null, "B":2, "BB":2, "BBB":2}

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        sync.receiveState(new JSONObject("{\"AAA\":3,\"BBB\":3,\"C\":3}"), true);
        // Pending diff is discarded
        JSONUtilTest.assertEquals(new JSONObject("{\"AAA\":3,\"BBB\":3,\"C\":3}"), sync.getSdkState());

        assertSyncedAfterRecvStateReset();
        JSONUtilTest.assertEquals(new JSONObject("{\"AAA\":3,\"BBB\":3,\"C\":3}"), sync.getSdkState());
    }

    @Test
    public void recvStateResetInflightPutSuccess() throws JSONException {
        singlePutSuccess();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"), sync.getSdkState());
        assertSynced();

        sync.put(new JSONObject("{\"AA\":2,\"AAA\":null,\"B\":2,\"BB\":2,\"BBB\":2}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":2,\"B\":2,\"BB\":2,\"BBB\":2}"), sync.getSdkState());

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"AA\":2,\"AAA\":null,\"B\":2,\"BB\":2,\"BBB\":2}")) {
            @Override
            protected void _serverPatchInstallation_do() throws Exception {
                sync.receiveState(new JSONObject("{\"AAA\":3,\"BBB\":3,\"C\":3}"), true);
                JSONUtilTest.assertEquals(new JSONObject("{\"AAA\":3,\"BBB\":3,\"C\":3}"), sync.getSdkState());
            }
        });
        JSONUtilTest.assertEquals(new JSONObject("{\"AAA\":3,\"BBB\":3,\"C\":3}"), sync.getSdkState());

        // Called (unfortunately!) succeeded, we have to revert the changed fields
        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"AA\":null,\"AAA\":3,\"B\":null,\"BB\":null,\"BBB\":3}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"AAA\":3,\"BBB\":3,\"C\":3}"), sync.getSdkState());

        assertSyncedAfterRecvStateReset();
        JSONUtilTest.assertEquals(new JSONObject("{\"AAA\":3,\"BBB\":3,\"C\":3}"), sync.getSdkState());
    }

    @Test
    public void recvStateResetInflightPutFailure() throws JSONException {
        singlePutSuccess();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"), sync.getSdkState());
        assertSynced();

        sync.put(new JSONObject("{\"AA\":2,\"AAA\":null,\"B\":2,\"BB\":2,\"BBB\":2}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":2,\"B\":2,\"BB\":2,\"BBB\":2}"), sync.getSdkState());

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndFailure(null, new JSONObject("{\"AA\":2,\"AAA\":null,\"B\":2,\"BB\":2,\"BBB\":2}")) {
            @Override
            protected void _serverPatchInstallation_do() throws Exception {
                sync.receiveState(new JSONObject("{\"AAA\":3,\"BBB\":3,\"C\":3}"), true);
                JSONUtilTest.assertEquals(new JSONObject("{\"AAA\":3,\"BBB\":3,\"C\":3}"), sync.getSdkState());
            }
        });
        JSONUtilTest.assertEquals(new JSONObject("{\"AAA\":3,\"BBB\":3,\"C\":3}"), sync.getSdkState());

        // Called (hopefully!) failed, we have no change to send
        assertSyncedPotentialNoopScheduledPatchCall();
        JSONUtilTest.assertEquals(new JSONObject("{\"AAA\":3,\"BBB\":3,\"C\":3}"), sync.getSdkState());
    }

    /* ********************************************************* *
     * Test receiveServerState() behavior wrt server patch calls *
     * ********************************************************* */

    private void assertSyncedAfterRecvSrvState() {
        // Be laxer with this final state check
        //assertNoopScheduledPatchCall();
        //assertSynced();
        assertSyncedPotentialNoopScheduledPatchCall();
    }

    @Test
    public void recvSrvStateNullObject() throws JSONException {
        assertSynced();
        JSONUtilTest.assertEquals(new JSONObject(), sync.getSdkState());
        sync.receiveServerState(null);
        JSONUtilTest.assertEquals(new JSONObject("{}"), sync.getSdkState());
        assertSyncedPotentialNoopScheduledPatchCall();
    }

    @Test
    public void recvSrvStateNullField() throws JSONException {
        assertSynced();
        JSONUtilTest.assertEquals(new JSONObject(), sync.getSdkState());
        sync.receiveServerState(new JSONObject("{\"A\":null}"));
        JSONUtilTest.assertEquals(new JSONObject("{}"), sync.getSdkState());
        assertSyncedPotentialNoopScheduledPatchCall();
    }

    @Test
    public void recvSrvStateFromInitialState() throws JSONException {
        assertSynced();
        JSONUtilTest.assertEquals(new JSONObject(), sync.getSdkState());
        sync.receiveServerState(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"));
        JSONUtilTest.assertEquals(new JSONObject("{}"), sync.getSdkState());

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"A\":null,\"AA\":null,\"AAA\":null}")));
        JSONUtilTest.assertEquals(new JSONObject("{}"), sync.getSdkState());
        assertSyncedAfterRecvSrvState();
    }

    @Test
    public void recvSrvStateFromNonEmptyState() throws JSONException {
        singlePutSuccess();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"), sync.getSdkState());
        assertSynced();

        sync.receiveServerState(new JSONObject("{\"A\":1,\"AAA\":2,\"B\":2,\"BB\":2}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"), sync.getSdkState());

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"AA\":1,\"AAA\":1,\"B\":null,\"BB\":null}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"), sync.getSdkState());
        assertSyncedAfterRecvSrvState();
    }

    @Test
    public void recvSrvStatePendingPut() throws JSONException {
        singlePutSuccess();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"), sync.getSdkState());
        assertSynced();

        sync.put(new JSONObject("{\"AA\":2,\"AAA\":null,\"B\":2,\"BB\":2,\"BBB\":2}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":2,\"B\":2,\"BB\":2,\"BBB\":2}"), sync.getSdkState());
        // Pending diff is: {"AA":2, "AAA":null, "B":2, "BB":2, "BBB":2}

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"AA\":2,\"AAA\":null,\"B\":2,\"BB\":2,\"BBB\":2}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":2,\"B\":2,\"BB\":2,\"BBB\":2}"), sync.getSdkState());

        sync.receiveServerState(new JSONObject("{\"AAA\":3,\"BB\":2,\"BBB\":3,\"C\":3}"));
        // "A" is added because it has vanished
        // "AA" is kept because it has vanished
        // "AAA" is kept because the underlying value still has not the desired value
        // "B" is kept because it has vanished
        // "BB" is removed because it has now the desired value
        // "C" is added because it has appeared
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":2,\"B\":2,\"BB\":2,\"BBB\":2}"), sync.getSdkState());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"A\":1,\"AA\":2,\"AAA\":null,\"B\":2,\"BBB\":2,\"C\":null}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":2,\"B\":2,\"BB\":2,\"BBB\":2}"), sync.getSdkState());

        assertSyncedAfterRecvSrvState();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":2,\"B\":2,\"BB\":2,\"BBB\":2}"), sync.getSdkState());
    }

    @Test
    public void recvSrvStateInflightPutSuccess() throws JSONException {
        singlePutSuccess();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"), sync.getSdkState());
        assertSynced();

        sync.put(new JSONObject("{\"AA\":2,\"AAA\":null,\"B\":2,\"BB\":2,\"BBB\":2}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":2,\"B\":2,\"BB\":2,\"BBB\":2}"), sync.getSdkState());

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"AA\":2,\"AAA\":null,\"B\":2,\"BB\":2,\"BBB\":2}")) {
            @Override
            protected void _serverPatchInstallation_do() throws Exception {
                sync.receiveServerState(new JSONObject("{\"AAA\":3,\"BB\":2,\"BBB\":3,\"C\":3}"));
                // "A" is added because it has vanished
                // "C" is added because it has appeared
                // The rest will be properly applied and will be discarded on success
                JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":2,\"B\":2,\"BB\":2,\"BBB\":2}"), sync.getSdkState());
            }
        });
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":2,\"B\":2,\"BB\":2,\"BBB\":2}"), sync.getSdkState());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"A\":1,\"C\":null}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":2,\"B\":2,\"BB\":2,\"BBB\":2}"), sync.getSdkState());

        assertSyncedAfterRecvSrvState();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":2,\"B\":2,\"BB\":2,\"BBB\":2}"), sync.getSdkState());
    }

    @Test
    public void recvSrvStateInflightPutFailure() throws JSONException {
        singlePutSuccess();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":1,\"AAA\":1}"), sync.getSdkState());
        assertSynced();

        sync.put(new JSONObject("{\"AA\":2,\"AAA\":null,\"B\":2,\"BB\":2,\"BBB\":2}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":2,\"B\":2,\"BB\":2,\"BBB\":2}"), sync.getSdkState());

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndFailure(null, new JSONObject("{\"AA\":2,\"AAA\":null,\"B\":2,\"BB\":2,\"BBB\":2}")) {
            @Override
            protected void _serverPatchInstallation_do() throws Exception {
                sync.receiveServerState(new JSONObject("{\"AAA\":3,\"BB\":2,\"BBB\":3,\"C\":3}"));
                // "A" is added because it has vanished
                // "AA" is kept because it has vanished
                // "AAA" is kept because the underlying value still has not the desired value
                // "B" is kept because it has vanished
                // "BB" is removed because it has now the desired value
                // "C" is added because it has appeared
                JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":2,\"B\":2,\"BB\":2,\"BBB\":2}"), sync.getSdkState());
            }
        });
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":2,\"B\":2,\"BB\":2,\"BBB\":2}"), sync.getSdkState());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"A\":1,\"AA\":2,\"AAA\":null,\"B\":2,\"BBB\":2,\"C\":null}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":2,\"B\":2,\"BB\":2,\"BBB\":2}"), sync.getSdkState());

        assertSyncedAfterRecvSrvState();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"AA\":2,\"B\":2,\"BB\":2,\"BBB\":2}"), sync.getSdkState());
    }

}
