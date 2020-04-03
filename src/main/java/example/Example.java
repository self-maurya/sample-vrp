
package example;

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Example {

    @SerializedName("status")
    @Expose
    private String status;
    @SerializedName("origin_addresses")
    @Expose
    private List<String> originAddresses = null;
    @SerializedName("destination_addresses")
    @Expose
    private List<String> destinationAddresses = null;
    @SerializedName("rows")
    @Expose
    private List<Row> rows = null;

    public String getStatus() {
        return status;
    }

    public List<String> getOriginAddresses() {
        return originAddresses;
    }

    public List<String> getDestinationAddresses() {
        return destinationAddresses;
    }

    public List<Row> getRows() {
        return rows;
    }
}
