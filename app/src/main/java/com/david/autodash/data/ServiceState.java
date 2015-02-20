package com.david.autodash.data;

/**
 * Created by davidhodge on 11/14/14.
 */
public class ServiceState {
    private RecordRequest recordRequest;

    private ServiceState() {
    }

    public boolean isRecording() {
        return recordRequest != null;
    }

    public static class Builder {
        private RecordRequest recordRequest;

        public Builder() {
        }

        public Builder(ServiceState state) {
        }

        public Builder recording(RecordRequest request) {
            this.recordRequest = request;
            return this;
        }

        public ServiceState build() {
            ServiceState state = new ServiceState();
            state.recordRequest = recordRequest;
            return state;
        }
    }
}
