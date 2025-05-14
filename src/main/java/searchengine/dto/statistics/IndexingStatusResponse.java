package searchengine.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;


@Data
@NoArgsConstructor
public class IndexingStatusResponse {
    private boolean result;
    private String error;

    public IndexingStatusResponse(boolean result, String error) {
        this.result = result;
        this.error = error;
    }

    public boolean getResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "{\"result\": " + this.result + ", \"error\": \"" + this.error + "\"}";
    }
}