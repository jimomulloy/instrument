package jomu.instrument.store;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

public class InstrumentSession {

	public enum InstrumentSessionState {
		INIT, RUNNING, STOPPED, FAILED
	};

	String id;
	String streamId;
	Instant dateTime;
	String userId;
	String inputAudioFileName;
	String inputAudioFilePath;
	String outputMidiFileName;
	String outputMidiFilePath;
	String statusCode;
	String statusMessage;

	transient final ReentrantLock lock = new ReentrantLock();
	InstrumentSessionState state = InstrumentSessionState.INIT;

	public InstrumentSession(String id) {
		setId(id);
	}

	public String getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}

	public String getStatusMessage() {
		return statusMessage;
	}

	public void setStatusMessage(String statusMessage) {
		this.statusMessage = statusMessage;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getStreamId() {
		return streamId;
	}

	public void setStreamId(String streamId) {
		this.streamId = streamId;
	}

	public Instant getDateTime() {
		return dateTime;
	}

	public void setDateTime(Instant dateTime) {
		this.dateTime = dateTime;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getInputAudioFileName() {
		return inputAudioFileName;
	}

	public void setInputAudioFileName(String inputAudioFileName) {
		this.inputAudioFileName = inputAudioFileName;
	}

	public String getInputAudioFilePath() {
		return inputAudioFilePath;
	}

	public void setInputAudioFilePath(String inputAudioFilePath) {
		this.inputAudioFilePath = inputAudioFilePath;
		if (inputAudioFilePath.lastIndexOf("/") > -1) {
			this.inputAudioFileName = inputAudioFilePath.substring(inputAudioFilePath.lastIndexOf("/") + 1);
		} else if (inputAudioFilePath.lastIndexOf("\\") > -1) {
			this.inputAudioFileName = inputAudioFilePath.substring(inputAudioFilePath.lastIndexOf("\\") + 1);
		} else {
			this.inputAudioFileName = inputAudioFilePath;
		}
	}

	public String getOutputMidiFileName() {
		return outputMidiFileName;
	}

	public void setOutputMidiFileName(String outputMidiFileName) {
		this.outputMidiFileName = outputMidiFileName;
	}

	public String getOutputMidiFilePath() {
		return outputMidiFilePath;
	}

	public void setOutputMidiFilePath(String outputMidiFilePath) {
		this.outputMidiFilePath = outputMidiFilePath;
		if (outputMidiFilePath.lastIndexOf("/") > -1) {
			this.outputMidiFileName = outputMidiFilePath.substring(outputMidiFilePath.lastIndexOf("/") + 1);
		} else if (outputMidiFilePath.lastIndexOf("\\") > -1) {
			this.outputMidiFileName = outputMidiFilePath.substring(outputMidiFilePath.lastIndexOf("\\") + 1);
		} else {
			this.outputMidiFileName = outputMidiFilePath;
		}
	}

	public void setState(InstrumentSessionState state) {
		lock.lock();
		try {
			this.state = state;
		} finally {
			lock.unlock();
		}
	}

	public InstrumentSessionState getState() {
		lock.lock();
		try {
			return this.state;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InstrumentSession other = (InstrumentSession) obj;
		return Objects.equals(id, other.id);
	}

	@Override
	public String toString() {
		return "InstrumentSession [streamId=" + streamId + ", dateTime=" + dateTime + ", id=" + id + ", userId="
				+ userId + ", inputAudioFileName=" + inputAudioFileName + ", inputAudioFilePath=" + inputAudioFilePath
				+ ", outputMidiFileName=" + outputMidiFileName + ", outputMidiFilePath=" + outputMidiFilePath + "]";
	}

}
