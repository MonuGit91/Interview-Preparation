package com.supai.app.services;

import org.springframework.stereotype.Service;

import com.supai.app.dto.request.DeleteReq;
import com.supai.app.otcsapis.AllVersions;
import com.supai.app.otcsapis.DeleteVersion;
import com.supai.app.otcsapis.OtcsToken;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeleteService {
//	private final ImpersonateUser impersonateUser;
	private final DeleteVersion deleteVersion;
	private final AllVersions allVersions;
	private final OtcsToken otcsToken;

	public void deleteAgent(DeleteReq request) throws Exception {
		String otcsTicket = otcsToken.getOtcsTicketJson().at("/ticket").asText();
		if (request.isDeleteAll()) {
			int maxVersionNo = allVersions.getMaxVersion(request.getNodeId(), otcsTicket);
			while (maxVersionNo > 1) {
				log.info("deleting version {} of node {}", maxVersionNo, request.getNodeId());
				deleteVersion.deleteVersion(otcsTicket, request.getNodeId(), (maxVersionNo--) + "");
			}
		} else {
			log.info("deletinf version {} of node {}", request.getVersionNo(), request.getNodeId());
			deleteVersion.deleteVersion(otcsTicket, request.getNodeId(), request.getVersionNo());
		}
	}

}
