package com.supai.app.ivapis.dto.xml;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.supai.app.ivapis.dto.xml.LogbookXml.IsoBanners;
import com.supai.app.ivapis.dto.xml.LogbookXml.PublishBanners;
import com.supai.app.ivapis.dto.xml.LogbookXml.StringValue;

@Component
public class XmlObjFactory {

	public XmlBanner1 createXmlBanner1(String topCenter, String bottomLeft, String bottomCenter, String bottomRight,
			String color, String fontName, String fontHeight, boolean includePageNo) {
		XmlBanner1.PublishBanners XmlBanner1 = new XmlBanner1.PublishBanners();
		
		/*
		 * by default page number will come but 
		 * if bottomRight contains content then page no will be override by content
		 * and if bottemRight is empty and includePageNo = false then we need to exclude pageNo
		 * below is the exclude page no if above explanation matches
		 */
		if(!includePageNo) {
			if (bottomRight == null || bottomRight.isEmpty()) {
				XmlBanner1.setBottomRight(new XmlBanner1.StringValue(""));
			}
		}
		
		if (fontName != null && !fontName.isEmpty()) {
			XmlBanner1.setFontName(new XmlBanner1.StringValue(fontName));
		}
		if (fontHeight != null && !fontHeight.isEmpty()) {
			XmlBanner1.setFontHeight(new XmlBanner1.StringValue(fontHeight));
		}
		if (color != null && !color.isEmpty()) {
			XmlBanner1.setColor(new XmlBanner1.StringValue(color));
		}

		XmlBanner1.setTopCenter(new XmlBanner1.StringValue(topCenter));
		XmlBanner1.setBottomLeft(new XmlBanner1.StringValue(bottomLeft));
		XmlBanner1.setBottomCenter(new XmlBanner1.StringValue(bottomCenter));

		if (bottomRight != null && !bottomRight.isEmpty()) {
			XmlBanner1.setBottomRight(new XmlBanner1.StringValue(bottomRight));
		}

		XmlBanner1.IsoBanners isoBanners = new XmlBanner1.IsoBanners(XmlBanner1);
		return new XmlBanner1(isoBanners);
	}

	public XmlBanner createXmlBanner(String topCenter, String bottomLeft, String bottomCenter, String bottomRight,
			String color, boolean includePageNo) {

		// 1. Build the list of Banners
		List<XmlBanner.Banner> banners = createBannerList(topCenter, bottomLeft, bottomCenter, bottomRight, color,
				includePageNo);

		// 2. Assemble the layers (Bottom to Top)
		XmlBanner.PublishBanners publishBanners = XmlBanner.PublishBanners.builder()
				.banners(banners).build();

		XmlBanner.IsoBannersPageGroup isoBannersPageGroup = XmlBanner.IsoBannersPageGroup.builder()
				.publishBanners(publishBanners)
				.build();

		return XmlBanner.builder().isoBannersPageGroup(isoBannersPageGroup).build();
	}

	/**
	 * Helper to build the 4 banner locations
	 */
	private List<XmlBanner.Banner> createBannerList(String topCenter, String bottomLeft, String bottomCenter,
			String bottomRight, String color, boolean includePageNo) {
		// Shared fragment settings
		XmlBanner.TextFragment.TextFragmentBuilder textFragment = XmlBanner.TextFragment.builder().index(1)
				.opacity("NaN").size(12).font("sans-serif").bold(false).italic(false).underline(false).color(color);

		XmlBanner.TextFragment.TextFragmentBuilder pageNoTextFragment = XmlBanner.TextFragment.builder().index(1)
				.opacity("NaN").size(12).font("sans-serif").bold(false).italic(false).underline(false).color("#000000");

		return List.of(new XmlBanner.Banner("TopCenter", textFragment.content(topCenter).build()),
				new XmlBanner.Banner("BottomLeft", textFragment.content(bottomLeft).build()),
				new XmlBanner.Banner("BottomCenter", textFragment.content(bottomCenter).build()),
				new XmlBanner.Banner("BottomRight",
						pageNoTextFragment.content(includePageNo ? XmlBanner.PAGE_NUMBER_TEMPLATE : "").build()));
	}

	public String getLogbookXmlString(String createBy, String createDate, String logbookNum) throws IOException {
		// Build the dynamic string for the BottomLeft attribute
		String bottomLeftText = String.format("Printed By: %s&#10;Printed On: %s&#10;Logbook Number: %s", createBy,
				createDate, logbookNum);

		// Populate the DTO hierarchy
		PublishBanners publishBanners = new PublishBanners();
		publishBanners.setBottomLeft(new StringValue(bottomLeftText));
		// Note: fontName, fontHeight, color, and bottomRight are

		IsoBanners isoBanners = new IsoBanners(publishBanners);
		LogbookXml logbookXml = new LogbookXml(isoBanners);

		// Serialize using XmlMapper to respect JacksonXml annotations
		XmlMapper xmlMapper = new XmlMapper();
		return xmlMapper.writeValueAsString(logbookXml);
	}

	public String xmlBase64Incoded(String xmlContent) {
		// 3. Base64 Encode
		String base64EncodedXML = Base64.getEncoder()
				.encodeToString(xmlContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
		return base64EncodedXML;
	}

	public XmlBanners1Json createXmlBanners1Json() {
		return XmlBanners1Json.builder()
				.topCenter(new XmlBanners1Json.BannerContent())
				.bottomLeft(new XmlBanners1Json.BannerContent())
				.bottomCenter(new XmlBanners1Json.BannerContent())
				.bottomRight(new XmlBanners1Json.BannerContent())
				.build();
	}
}
