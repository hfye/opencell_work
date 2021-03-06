package org.meveo.api.rest.catalog;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.catalog.ChannelDto;
import org.meveo.api.dto.response.catalog.GetChannelResponseDto;
import org.meveo.api.rest.IBaseRs;

@Path("/catalog/channel")
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })

public interface ChannelRs extends IBaseRs {


    @Path("/")
    @POST
    ActionStatus create(ChannelDto postData);

    @Path("/")
    @PUT
    ActionStatus update(ChannelDto postData);

    @GET
    @Path("/")
    GetChannelResponseDto find(@QueryParam("channelCode") String channelCode);

    @Path("/")
    @DELETE
    ActionStatus delete(@QueryParam("channelCode") String channelCode);

    @Path("/createOrUpdate")
    @POST
    ActionStatus createOrUpdate(ChannelDto postData);
}
