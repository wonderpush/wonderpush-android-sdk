package com.wonderpush.sdk;

import junit.framework.Assert;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class SyncTest {

    private Sync sync;

    private static abstract class MockServer implements Sync.Server {

        private boolean called = false;

        boolean isCalled() {
            return called;
        }

        @Override
        public final void patchInstallation(JSONObject diff, Sync.ResponseHandler handler) {
            Assert.assertFalse("Server mock object must not be reused", called);
            called = true;
            _patchInstallation_diff(diff);
            try {
                _patchInstallation_do();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            _patchInstallation_handler(handler);
        }

        protected void _patchInstallation_diff(JSONObject diff) {}
        protected void _patchInstallation_do() throws Exception {}
        protected void _patchInstallation_handler(Sync.ResponseHandler handler) {}
    }

    private class ServerAssertNotCalled extends MockServer {

        @Override
        public void _patchInstallation_diff(JSONObject diff) {
            Assert.fail("Server.patchInstallation should not be called\nGot diff: " + diff + "\nSync state: " + sync);
        }

    }

    private static class ServerAssertDiffAndSuccess extends MockServer {

        private String message;
        private JSONObject expectedDiff;

        ServerAssertDiffAndSuccess(final String message, final JSONObject expectedDiff) {
            this.message = message;
            this.expectedDiff = expectedDiff;
        }

        @Override
        public void _patchInstallation_diff(JSONObject diff) {
            JSONUtilTest.assertEquals(message, expectedDiff, diff);
        }

        @Override
        public void _patchInstallation_handler(Sync.ResponseHandler handler) {
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
        public void _patchInstallation_diff(JSONObject diff) {
            JSONUtilTest.assertEquals(message, expectedDiff, diff);
        }

        @Override
        public void _patchInstallation_handler(Sync.ResponseHandler handler) {
            handler.onFailure();
        }

    }

    @Before
    public void setup() {
        sync = new Sync();
    }

    private void assertSynced() throws JSONException {
        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertFalse(sync.hasScheduledPatchCall());
        MockServer server = new ServerAssertNotCalled();
        Assert.assertFalse(sync.performScheduledPatchCall(server));
        Assert.assertFalse(server.isCalled());
    }

    private void assertSyncedPotentialNoopScheduledPatchCall() throws JSONException {
        assertPotentialNoopScheduledPatchCall();
        assertSynced();
    }

    private void assertPotentialNoopScheduledPatchCall() throws JSONException {
        if (sync.hasScheduledPatchCall()) {
            assertNoopScheduledPatchCall();
        } else {
            Assert.assertTrue(true);
        }
    }

    private void assertNoopScheduledPatchCall() throws JSONException {
        Assert.assertFalse(sync.hasInflightPatchCall());
        MockServer server = new ServerAssertNotCalled();
        Assert.assertTrue(sync.performScheduledPatchCall(server));
        Assert.assertFalse(server.isCalled());
    }

    private void assertPerformScheduledPatchCallWith(MockServer server) throws JSONException {
        Assert.assertTrue(sync.performScheduledPatchCall(server));
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
    public void singlePutSuccess() throws JSONException {
        sync.put(new JSONObject("{\"A\":1}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1}"), sync.getSdkState());

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"A\":1}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1}"), sync.getSdkState());

        assertSyncedPotentialNoopScheduledPatchCall();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1}"), sync.getSdkState());
    }

    @Test
    public void singlePutFailure() throws JSONException {
        sync.put(new JSONObject("{\"A\":1}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1}"), sync.getSdkState());

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndFailure(null, new JSONObject("{\"A\":1}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1}"), sync.getSdkState());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"A\":1}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1}"), sync.getSdkState());

        assertSyncedPotentialNoopScheduledPatchCall();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1}"), sync.getSdkState());
    }

    @Test
    public void subsequentSinglePutSuccess() throws JSONException {
        singlePutSuccess();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1}"), sync.getSdkState());
        sync.put(new JSONObject("{\"B\":2}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2}"), sync.getSdkState());

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"B\":2}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2}"), sync.getSdkState());

        assertSyncedPotentialNoopScheduledPatchCall();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2}"), sync.getSdkState());
    }

    @Test
    public void subsequentSinglePutFailure() throws JSONException {
        singlePutSuccess();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1}"), sync.getSdkState());
        sync.put(new JSONObject("{\"B\":2}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2}"), sync.getSdkState());

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndFailure(null, new JSONObject("{\"B\":2}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2}"), sync.getSdkState());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"B\":2}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2}"), sync.getSdkState());

        assertSyncedPotentialNoopScheduledPatchCall();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2}"), sync.getSdkState());
    }

    @Test
    public void pendingPutSuccess() throws JSONException {
        sync.put(new JSONObject("{\"A\":1}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1}"), sync.getSdkState());

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        sync.put(new JSONObject("{\"B\":2}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2}"), sync.getSdkState());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"A\":1,\"B\":2}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2}"), sync.getSdkState());

        assertSyncedPotentialNoopScheduledPatchCall();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2}"), sync.getSdkState());
    }

    @Test
    public void pendingPutFailure() throws JSONException {
        sync.put(new JSONObject("{\"A\":1}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1}"), sync.getSdkState());

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        sync.put(new JSONObject("{\"B\":2}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2}"), sync.getSdkState());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndFailure(null, new JSONObject("{\"A\":1,\"B\":2}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2}"), sync.getSdkState());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"A\":1,\"B\":2}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2}"), sync.getSdkState());

        assertSyncedPotentialNoopScheduledPatchCall();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2}"), sync.getSdkState());
    }

    @Test
    public void putWhileInflightSuccess() throws JSONException {
        sync.put(new JSONObject("{\"A\":1}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1}"), sync.getSdkState());

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"A\":1}")) {
            @Override
            protected void _patchInstallation_do() throws Exception {
                sync.put(new JSONObject("{\"B\":2}"));
                JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2}"), sync.getSdkState());
            }
        });
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2}"), sync.getSdkState());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"B\":2}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2}"), sync.getSdkState());

        assertSyncedPotentialNoopScheduledPatchCall();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2}"), sync.getSdkState());
    }

    @Test
    public void putWhileInflightFailure() throws JSONException {
        sync.put(new JSONObject("{\"A\":1}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1}"), sync.getSdkState());

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndFailure(null, new JSONObject("{\"A\":1}")) {
            @Override
            protected void _patchInstallation_do() throws Exception {
                sync.put(new JSONObject("{\"B\":2}"));
                JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2}"), sync.getSdkState());
            }
        });
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2}"), sync.getSdkState());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"A\":1,\"B\":2}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2}"), sync.getSdkState());

        assertSyncedPotentialNoopScheduledPatchCall();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2}"), sync.getSdkState());
    }

    /* *********************************************** *
     * Test recvDiff() behavior wrt server patch calls *
     * *********************************************** */

    private void assertSyncedAfterRecvDiff() throws JSONException {
        // Be laxer with this final state check
        //assertNoopScheduledPatchCall();
        //assertSynced();
        assertSyncedPotentialNoopScheduledPatchCall();
    }

    @Test
    public void recvDiffFromInitialState() throws JSONException {
        assertSynced();
        JSONUtilTest.assertEquals(new JSONObject(), sync.getSdkState());
        sync.recvDiff(new JSONObject("{\"A\":1}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1}"), sync.getSdkState());
        assertSyncedAfterRecvDiff();
    }

    @Test
    public void recvDiffFromNonEmptyState() throws JSONException {
        singlePutSuccess();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1}"), sync.getSdkState());
        assertSynced();

        sync.recvDiff(new JSONObject("{\"B\":2}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2}"), sync.getSdkState());
        assertSyncedAfterRecvDiff();
    }

    @Test
    public void recvDiffPendingPutSuccess() throws JSONException {
        singlePutSuccess();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1}"), sync.getSdkState());
        assertSynced();

        sync.put(new JSONObject("{\"B\":2}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2}"), sync.getSdkState());

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        sync.recvDiff(new JSONObject("{\"C\":3}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2,\"C\":3}"), sync.getSdkState());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"B\":2}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2,\"C\":3}"), sync.getSdkState());

        assertSyncedPotentialNoopScheduledPatchCall();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2,\"C\":3}"), sync.getSdkState());
    }

    @Test
    public void recvDiffPendingPutFailure() throws JSONException {
        singlePutSuccess();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1}"), sync.getSdkState());
        assertSynced();

        sync.put(new JSONObject("{\"B\":2}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2}"), sync.getSdkState());

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        sync.recvDiff(new JSONObject("{\"C\":3}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2,\"C\":3}"), sync.getSdkState());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndFailure(null, new JSONObject("{\"B\":2}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2,\"C\":3}"), sync.getSdkState());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"B\":2}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2,\"C\":3}"), sync.getSdkState());

        assertSyncedPotentialNoopScheduledPatchCall();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2,\"C\":3}"), sync.getSdkState());
    }

    @Test
    public void recvDiffInflightPutSuccess() throws JSONException {
        singlePutSuccess();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1}"), sync.getSdkState());
        assertSynced();

        sync.put(new JSONObject("{\"B\":2}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2}"), sync.getSdkState());

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"B\":2}")) {
            @Override
            protected void _patchInstallation_do() throws Exception {
                sync.recvDiff(new JSONObject("{\"C\":3}"));
                JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2,\"C\":3}"), sync.getSdkState());
            }
        });
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2,\"C\":3}"), sync.getSdkState());

        assertSyncedAfterRecvDiff();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2,\"C\":3}"), sync.getSdkState());
    }

    @Test
    public void recvDiffInflightPutFailure() throws JSONException {
        singlePutSuccess();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1}"), sync.getSdkState());
        assertSynced();

        sync.put(new JSONObject("{\"B\":2}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2}"), sync.getSdkState());

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndFailure(null, new JSONObject("{\"B\":2}")) {
            @Override
            protected void _patchInstallation_do() throws Exception {
                sync.recvDiff(new JSONObject("{\"C\":3}"));
                JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2,\"C\":3}"), sync.getSdkState());
            }
        });
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2,\"C\":3}"), sync.getSdkState());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"B\":2}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2,\"C\":3}"), sync.getSdkState());

        assertSyncedPotentialNoopScheduledPatchCall();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2,\"C\":3}"), sync.getSdkState());
    }

    /* ************************************************ *
     * Test recvState() behavior wrt server patch calls *
     * ************************************************ */

    private void assertSyncedAfterRecvState() throws JSONException {
        // Be laxer with this final state check
        //assertNoopScheduledPatchCall();
        //assertSynced();
        assertSyncedPotentialNoopScheduledPatchCall();
    }

    @Test
    public void rectStateFromInitialState() throws JSONException {
        assertSynced();
        JSONUtilTest.assertEquals(new JSONObject(), sync.getSdkState());
        sync.recvState(new JSONObject("{\"A\":1}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1}"), sync.getSdkState());
        assertSyncedAfterRecvState();
    }

    @Test
    public void recvStateFromNonEmptyState() throws JSONException {
        singlePutSuccess();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1}"), sync.getSdkState());
        assertSynced();

        sync.recvState(new JSONObject("{\"A\":1,\"B\":2}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2}"), sync.getSdkState());
        assertSyncedAfterRecvState();
    }

    @Test
    public void recvStatePendingPutSuccess() throws JSONException {
        singlePutSuccess();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1}"), sync.getSdkState());
        assertSynced();

        sync.put(new JSONObject("{\"B\":2}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2}"), sync.getSdkState());

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        sync.recvState(new JSONObject("{\"A\":1,\"C\":3}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2,\"C\":3}"), sync.getSdkState());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"B\":2}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2,\"C\":3}"), sync.getSdkState());

        assertSyncedPotentialNoopScheduledPatchCall();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2,\"C\":3}"), sync.getSdkState());
    }

    @Test
    public void recvStatePendingPutFailure() throws JSONException {
        singlePutSuccess();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1}"), sync.getSdkState());
        assertSynced();

        sync.put(new JSONObject("{\"B\":2}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2}"), sync.getSdkState());

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        sync.recvState(new JSONObject("{\"A\":1,\"C\":3}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2,\"C\":3}"), sync.getSdkState());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndFailure(null, new JSONObject("{\"B\":2}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2,\"C\":3}"), sync.getSdkState());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"B\":2}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2,\"C\":3}"), sync.getSdkState());

        assertSyncedPotentialNoopScheduledPatchCall();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2,\"C\":3}"), sync.getSdkState());
    }

    @Test
    public void recvStateInflightPutSuccess() throws JSONException {
        singlePutSuccess();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1}"), sync.getSdkState());
        assertSynced();

        sync.put(new JSONObject("{\"B\":2}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2}"), sync.getSdkState());

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"B\":2}")) {
            @Override
            protected void _patchInstallation_do() throws Exception {
                sync.recvState(new JSONObject("{\"A\":1,\"C\":3}"));
                JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2,\"C\":3}"), sync.getSdkState());
            }
        });
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2,\"C\":3}"), sync.getSdkState());

        assertSyncedAfterRecvState();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2,\"C\":3}"), sync.getSdkState());
    }

    @Test
    public void recvStateInflightPutFailure() throws JSONException {
        singlePutSuccess();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1}"), sync.getSdkState());
        assertSynced();

        sync.put(new JSONObject("{\"B\":2}"));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2}"), sync.getSdkState());

        Assert.assertFalse(sync.hasInflightPatchCall());
        Assert.assertTrue(sync.hasScheduledPatchCall());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndFailure(null, new JSONObject("{\"B\":2}")) {
            @Override
            protected void _patchInstallation_do() throws Exception {
                sync.recvState(new JSONObject("{\"A\":1,\"C\":3}"));
                JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2,\"C\":3}"), sync.getSdkState());
            }
        });
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2,\"C\":3}"), sync.getSdkState());

        assertPerformScheduledPatchCallWith(new ServerAssertDiffAndSuccess(null, new JSONObject("{\"B\":2}")));
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2,\"C\":3}"), sync.getSdkState());

        assertSyncedPotentialNoopScheduledPatchCall();
        JSONUtilTest.assertEquals(new JSONObject("{\"A\":1,\"B\":2,\"C\":3}"), sync.getSdkState());
    }

}
