package com.genersoft.iot.vmp.gb28181.transmit.cmd;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.UUID;

import javax.sip.Dialog;
import javax.sip.InvalidArgumentException;
import javax.sip.PeerUnavailableException;
import javax.sip.SipFactory;
import javax.sip.SipProvider;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.SubjectHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Request;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.session.InfoCseqCache;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;

/**
 * @Description:摄像头命令request创造器 TODO 冗余代码太多待优化
 * @author: swwheihei
 * @date: 2020年5月6日 上午9:29:02
 */
@Component
public class SIPRequestHeaderProvider {

	@Autowired
	private SipConfig sipConfig;

	@Autowired
	private SipFactory sipFactory;

	@Autowired
	private VideoStreamSessionManager streamSession;

	@Autowired
	@Qualifier(value = "tcpSipProvider")
	private SipProvider tcpSipProvider;

	@Autowired
	@Qualifier(value = "udpSipProvider")
	private SipProvider udpSipProvider;

	public Request createMessageRequest(Device device, String content, String viaTag, String fromTag, String toTag)
			throws ParseException, InvalidArgumentException, PeerUnavailableException {
		Request request = null;
		// sipuri
		SipURI requestURI = sipFactory.createAddressFactory().createSipURI(device.getDeviceId(),
				device.getHostAddress());
		// via
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = sipFactory.createHeaderFactory().createViaHeader(sipConfig.getSipIp(),
				sipConfig.getSipPort(), device.getTransport(), viaTag);
		viaHeader.setRPort();
		viaHeaders.add(viaHeader);
		// from
		SipURI fromSipURI = sipFactory.createAddressFactory().createSipURI(sipConfig.getSipId(),
				sipConfig.getSipIp() + ":" + sipConfig.getSipPort());
		Address fromAddress = sipFactory.createAddressFactory().createAddress(fromSipURI);
		FromHeader fromHeader = sipFactory.createHeaderFactory().createFromHeader(fromAddress, fromTag);
		// to
		SipURI toSipURI = sipFactory.createAddressFactory().createSipURI(device.getDeviceId(),
				sipConfig.getSipDomain());
		Address toAddress = sipFactory.createAddressFactory().createAddress(toSipURI);
		ToHeader toHeader = sipFactory.createHeaderFactory().createToHeader(toAddress, toTag);
		// callid
		CallIdHeader callIdHeader = device.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
				: udpSipProvider.getNewCallId();
		// Forwards
		MaxForwardsHeader maxForwards = sipFactory.createHeaderFactory().createMaxForwardsHeader(70);
		// ceq
		CSeqHeader cSeqHeader = sipFactory.createHeaderFactory().createCSeqHeader(1L, Request.MESSAGE);

		request = sipFactory.createMessageFactory().createRequest(requestURI, Request.MESSAGE, callIdHeader, cSeqHeader,
				fromHeader, toHeader, viaHeaders, maxForwards);
		ContentTypeHeader contentTypeHeader = sipFactory.createHeaderFactory().createContentTypeHeader("APPLICATION",
				"MANSCDP+xml");
		request.setContent(content, contentTypeHeader);
		return request;
	}

	public Request createInviteRequest(Device device, String channelId, String content, String viaTag, String fromTag,
			String toTag, String ssrc) throws ParseException, InvalidArgumentException, PeerUnavailableException {
		Request request = null;
		// 请求行
		SipURI requestLine = sipFactory.createAddressFactory().createSipURI(channelId, device.getHostAddress());
		// via
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = sipFactory.createHeaderFactory().createViaHeader(device.getIp(), device.getPort(),
				device.getTransport(), viaTag);
		viaHeader.setRPort();
		viaHeaders.add(viaHeader);

		// from
		SipURI fromSipURI = sipFactory.createAddressFactory().createSipURI(sipConfig.getSipId(),
				sipConfig.getSipDomain());
		Address fromAddress = sipFactory.createAddressFactory().createAddress(fromSipURI);
		FromHeader fromHeader = sipFactory.createHeaderFactory().createFromHeader(fromAddress, fromTag); // 必须要有标记，否则无法创建会话，无法回应ack
		// to
		SipURI toSipURI = sipFactory.createAddressFactory().createSipURI(channelId, sipConfig.getSipDomain());
		Address toAddress = sipFactory.createAddressFactory().createAddress(toSipURI);
		ToHeader toHeader = sipFactory.createHeaderFactory().createToHeader(toAddress, null);

		// callid
		CallIdHeader callIdHeader = null;
		if (device.getTransport().equals("TCP")) {
			callIdHeader = tcpSipProvider.getNewCallId();
		}
		if (device.getTransport().equals("UDP")) {
			callIdHeader = udpSipProvider.getNewCallId();
		}

		// Forwards
		MaxForwardsHeader maxForwards = sipFactory.createHeaderFactory().createMaxForwardsHeader(70);

		// ceq
		CSeqHeader cSeqHeader = sipFactory.createHeaderFactory().createCSeqHeader(1L, Request.INVITE);
		request = sipFactory.createMessageFactory().createRequest(requestLine, Request.INVITE, callIdHeader, cSeqHeader,
				fromHeader, toHeader, viaHeaders, maxForwards);

		Address concatAddress = sipFactory.createAddressFactory().createAddress(sipFactory.createAddressFactory()
				.createSipURI(sipConfig.getSipId(), sipConfig.getSipIp() + ":" + sipConfig.getSipPort()));
		// Address concatAddress =
		// sipFactory.createAddressFactory().createAddress(sipFactory.createAddressFactory().createSipURI(sipConfig.getSipId(),
		// device.getHost().getIp()+":"+device.getHost().getPort()));
		request.addHeader(sipFactory.createHeaderFactory().createContactHeader(concatAddress));
		// Subject
		SubjectHeader subjectHeader = sipFactory.createHeaderFactory()
				.createSubjectHeader(String.format("%s:%s,%s:%s", channelId, ssrc, sipConfig.getSipId(), 0));
		request.addHeader(subjectHeader);
		ContentTypeHeader contentTypeHeader = sipFactory.createHeaderFactory().createContentTypeHeader("APPLICATION",
				"SDP");
		request.setContent(content, contentTypeHeader);
		return request;
	}

	public Request createPlaybackInviteRequest(Device device, String channelId, String content, String viaTag,
			String fromTag, String toTag) throws ParseException, InvalidArgumentException, PeerUnavailableException {
		Request request = null;
		// 请求行
		SipURI requestLine = sipFactory.createAddressFactory().createSipURI(device.getDeviceId(),
				device.getHostAddress());
		// via
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = sipFactory.createHeaderFactory().createViaHeader(device.getIp(), device.getPort(),
				device.getTransport(), "z9hG4bK" + UUID.randomUUID().toString().replace("-", "").substring(0, 9));
		viaHeader.setRPort();
		viaHeaders.add(viaHeader);
		// from
		SipURI fromSipURI = sipFactory.createAddressFactory().createSipURI(sipConfig.getSipId(),
				sipConfig.getSipDomain());
		Address fromAddress = sipFactory.createAddressFactory().createAddress(fromSipURI);
		FromHeader fromHeader = sipFactory.createHeaderFactory().createFromHeader(fromAddress, fromTag); // 必须要有标记，否则无法创建会话，无法回应ack
		// to
		SipURI toSipURI = sipFactory.createAddressFactory().createSipURI(channelId, sipConfig.getSipDomain());
		Address toAddress = sipFactory.createAddressFactory().createAddress(toSipURI);
		ToHeader toHeader = sipFactory.createHeaderFactory().createToHeader(toAddress, null);

		// callid
		CallIdHeader callIdHeader = null;
		if (device.getTransport().equals("TCP")) {
			callIdHeader = tcpSipProvider.getNewCallId();
		}
		if (device.getTransport().equals("UDP")) {
			callIdHeader = udpSipProvider.getNewCallId();
		}

		// Forwards
		MaxForwardsHeader maxForwards = sipFactory.createHeaderFactory().createMaxForwardsHeader(70);

		// ceq
		CSeqHeader cSeqHeader = sipFactory.createHeaderFactory().createCSeqHeader(1L, Request.INVITE);
		request = sipFactory.createMessageFactory().createRequest(requestLine, Request.INVITE, callIdHeader, cSeqHeader,
				fromHeader, toHeader, viaHeaders, maxForwards);

		Address concatAddress = sipFactory.createAddressFactory().createAddress(sipFactory.createAddressFactory()
				.createSipURI(sipConfig.getSipId(), sipConfig.getSipIp() + ":" + sipConfig.getSipPort()));
		// Address concatAddress =
		// sipFactory.createAddressFactory().createAddress(sipFactory.createAddressFactory().createSipURI(sipConfig.getSipId(),
		// device.getHost().getIp()+":"+device.getHost().getPort()));
		request.addHeader(sipFactory.createHeaderFactory().createContactHeader(concatAddress));

		ContentTypeHeader contentTypeHeader = sipFactory.createHeaderFactory().createContentTypeHeader("APPLICATION",
				"SDP");
		request.setContent(content, contentTypeHeader);
		return request;
	}

	// banmajio
	public Request createInfoRequest(Device device, StreamInfo streamInfo, String content)
			throws PeerUnavailableException, ParseException, InvalidArgumentException {
		Request request = null;
		Dialog dialog = streamSession.get(streamInfo.getStreamId()).getDialog();

		SipURI requestLine = sipFactory.createAddressFactory().createSipURI(device.getDeviceId(),
				device.getHostAddress());
		// via
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = sipFactory.createHeaderFactory().createViaHeader(device.getIp(), device.getPort(),
				device.getTransport(), "z9hG4bK" + UUID.randomUUID().toString().replace("-", "").substring(0, 9));
		viaHeader.setRPort();
		viaHeaders.add(viaHeader);
		// from
		SipURI fromSipURI = sipFactory.createAddressFactory().createSipURI(sipConfig.getSipId(),
				sipConfig.getSipDomain());
		Address fromAddress = sipFactory.createAddressFactory().createAddress(fromSipURI);
		FromHeader fromHeader = sipFactory.createHeaderFactory().createFromHeader(fromAddress, "playback");
		// to
		SipURI toSipURI = sipFactory.createAddressFactory().createSipURI(streamInfo.getChannelId(),
				sipConfig.getSipDomain());
		Address toAddress = sipFactory.createAddressFactory().createAddress(toSipURI);
		ToHeader toHeader = sipFactory.createHeaderFactory().createToHeader(toAddress, dialog.getRemoteTag());

		// callid
		CallIdHeader callIdHeader = dialog.getCallId();

		// Forwards
		MaxForwardsHeader maxForwards = sipFactory.createHeaderFactory().createMaxForwardsHeader(70);

		// ceq
		CSeqHeader cSeqHeader = sipFactory.createHeaderFactory()
				.createCSeqHeader(InfoCseqCache.CSEQCACHE.get(streamInfo.getStreamId()), Request.INFO);

		request = sipFactory.createMessageFactory().createRequest(requestLine, Request.INFO, callIdHeader, cSeqHeader,
				fromHeader, toHeader, viaHeaders, maxForwards);
		Address concatAddress = sipFactory.createAddressFactory().createAddress(sipFactory.createAddressFactory()
				.createSipURI(sipConfig.getSipId(), sipConfig.getSipIp() + ":" + sipConfig.getSipPort()));
		request.addHeader(sipFactory.createHeaderFactory().createContactHeader(concatAddress));

		ContentTypeHeader contentTypeHeader = sipFactory.createHeaderFactory().createContentTypeHeader("Application",
				"MANSRTSP");
		request.setContent(content, contentTypeHeader);
		return request;
	}
}
