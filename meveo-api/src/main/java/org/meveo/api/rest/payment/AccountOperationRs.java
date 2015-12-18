package org.meveo.api.rest.payment;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.payment.AccountOperationDto;
import org.meveo.api.dto.payment.LitigationRequestDto;
import org.meveo.api.dto.payment.MatchOperationRequestDto;
import org.meveo.api.dto.payment.UnMatchingOperationRequestDto;
import org.meveo.api.dto.response.payment.AccountOperationsResponseDto;
import org.meveo.api.rest.IBaseRs;
import org.meveo.api.rest.security.RSSecured;

/**
 * @author Edward P. Legaspi
 **/
@Path("/accountOperation")
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@RSSecured
public interface AccountOperationRs extends IBaseRs {

	@POST
	@Path("/")
	ActionStatus create(AccountOperationDto postData);

	@GET
	@Path("/list")
	AccountOperationsResponseDto list(@QueryParam("customerAccountCode") String customerAccountCode);
	
	@POST
	@Path("/")
	ActionStatus matchOperations(MatchOperationRequestDto postData);
	
	@POST
	@Path("/")
	ActionStatus unMatchingOperations(UnMatchingOperationRequestDto postData);
	
	@POST
	@Path("/")
	ActionStatus addLitigation(LitigationRequestDto postData);
	
	@POST
	@Path("/")
	ActionStatus cancelLitigation(LitigationRequestDto postData);


}
