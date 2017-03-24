/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.smartloli.kafka.eagle.web.controller;

import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.smartloli.kafka.eagle.common.domain.PageParamDomain;
import org.smartloli.kafka.eagle.common.util.ConstantUtils;
import org.smartloli.kafka.eagle.common.util.GzipUtils;
import org.smartloli.kafka.eagle.common.util.SystemConfigUtils;
import org.smartloli.kafka.eagle.web.service.ConsumerService;

/**
 * Kafka consumer controller to viewer data.
 * 
 * @author smartloli.
 *
 *         Created by Sep 6, 2016.
 *         
 *         Update by hexiang 20170216
 */
@Controller
public class ConsumersController {

	/** Kafka consumer service interface. */
	@Autowired
	private ConsumerService consumerService;

	/** Consumer viewer. */
	@RequestMapping(value = "/consumers", method = RequestMethod.GET)
	public ModelAndView consumersView(HttpSession session) {
		ModelAndView mav = new ModelAndView();
		mav.setViewName("/consumers/consumers");
		return mav;
	}

	/** Get consumer data by ajax. */
	@RequestMapping(value = "/consumers/info/ajax", method = RequestMethod.GET)
	public void consumersGraphAjax(HttpServletResponse response, HttpServletRequest request) {
		response.setContentType("text/html;charset=utf-8");
		response.setCharacterEncoding("utf-8");
		response.setHeader("Charset", "utf-8");
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Content-Encoding", "gzip");

		HttpSession session = request.getSession();
		String clusterAlias = session.getAttribute(ConstantUtils.SessionAlias.CLUSTER_ALIAS).toString();
		
		try {
			String formatter = SystemConfigUtils.getProperty("kafka.eagle.offset.storage");
			byte[] output = GzipUtils.compressToByte(consumerService.getActiveTopic(clusterAlias,formatter));
			response.setContentLength(output.length);
			OutputStream out = response.getOutputStream();
			out.write(output);

			out.flush();
			out.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/** Get consumer datasets by ajax. */
	@RequestMapping(value = "/consumer/list/table/ajax", method = RequestMethod.GET)
	public void consumerTableAjax(HttpServletResponse response, HttpServletRequest request) {
		response.setContentType("text/html;charset=utf-8");
		response.setCharacterEncoding("utf-8");
		response.setHeader("Charset", "utf-8");
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Content-Encoding", "gzip");

		String aoData = request.getParameter("aoData");
		JSONArray params = JSON.parseArray(aoData);
		int sEcho = 0, iDisplayStart = 0, iDisplayLength = 0;
		String search = "";
		for (Object object : params) {
			JSONObject param = (JSONObject) object;
			if ("sEcho".equals(param.getString("name"))) {
				sEcho = param.getIntValue("value");
			} else if ("iDisplayStart".equals(param.getString("name"))) {
				iDisplayStart = param.getIntValue("value");
			} else if ("iDisplayLength".equals(param.getString("name"))) {
				iDisplayLength = param.getIntValue("value");
			} else if ("sSearch".equals(param.getString("name"))) {
				search = param.getString("value");
			}
		}

		PageParamDomain page = new PageParamDomain();
		page.setSearch(search);
		page.setiDisplayLength(iDisplayLength);
		page.setiDisplayStart(iDisplayStart);

		HttpSession session = request.getSession();
		String clusterAlias = session.getAttribute(ConstantUtils.SessionAlias.CLUSTER_ALIAS).toString();
		
		String formatter = SystemConfigUtils.getProperty("kafka.eagle.offset.storage");
		int count = consumerService.getConsumerCount(clusterAlias,formatter);
		JSONArray consumers = JSON.parseArray(consumerService.getConsumer(clusterAlias,formatter, page));
		JSONArray aaDatas = new JSONArray();
		for (Object object : consumers) {
			JSONObject consumer = (JSONObject) object;
			JSONObject obj = new JSONObject();
			obj.put("id", consumer.getInteger("id"));
			obj.put("group", "<a class='link' href='#" + consumer.getString("group") + "'>" + consumer.getString("group") + "</a>");
			obj.put("topic", consumer.getString("topic").length() > 50 ? consumer.getString("topic").substring(0, 50) + "..." : consumer.getString("topic"));
			obj.put("consumerNumber", consumer.getInteger("consumerNumber"));
			int activerNumber = consumer.getInteger("activeNumber");
			if (activerNumber > 0) {
				obj.put("activeNumber", "<a class='btn btn-success btn-xs'>" + consumer.getInteger("activeNumber") + "</a>");
			} else {
				obj.put("activeNumber", "<a class='btn btn-danger btn-xs'>" + consumer.getInteger("activeNumber") + "</a>");
			}
			aaDatas.add(obj);
		}

		JSONObject target = new JSONObject();
		target.put("sEcho", sEcho);
		target.put("iTotalRecords", count);
		target.put("iTotalDisplayRecords", count);
		target.put("aaData", aaDatas);
		try {
			byte[] output = GzipUtils.compressToByte(target.toJSONString());
			response.setContentLength(output.length);
			OutputStream out = response.getOutputStream();
			out.write(output);

			out.flush();
			out.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/** Get consumer data through group by ajax. */
	@RequestMapping(value = "/consumer/{group}/table/ajax", method = RequestMethod.GET)
	public void consumerTableListAjax(@PathVariable("group") String group, HttpServletResponse response, HttpServletRequest request) {
		response.setContentType("text/html;charset=utf-8");
		response.setCharacterEncoding("utf-8");
		response.setHeader("Charset", "utf-8");
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Content-Encoding", "gzip");

		String aoData = request.getParameter("aoData");
		JSONArray params = JSON.parseArray(aoData);
		int sEcho = 0, iDisplayStart = 0, iDisplayLength = 0;
		for (Object object : params) {
			JSONObject param = (JSONObject) object;
			if ("sEcho".equals(param.getString("name"))) {
				sEcho = param.getIntValue("value");
			} else if ("iDisplayStart".equals(param.getString("name"))) {
				iDisplayStart = param.getIntValue("value");
			} else if ("iDisplayLength".equals(param.getString("name"))) {
				iDisplayLength = param.getIntValue("value");
			}
		}

		HttpSession session = request.getSession();
		String clusterAlias = session.getAttribute(ConstantUtils.SessionAlias.CLUSTER_ALIAS).toString();
		
		String formatter = SystemConfigUtils.getProperty("kafka.eagle.offset.storage");
		JSONArray consumerDetails = JSON.parseArray(consumerService.getConsumerDetail(clusterAlias,formatter, group));
		int offset = 0;
		JSONArray aaDatas = new JSONArray();
		for (Object object : consumerDetails) {
			JSONObject consumerDetail = (JSONObject) object;
			if (offset < (iDisplayLength + iDisplayStart) && offset >= iDisplayStart) {
				JSONObject obj = new JSONObject();
				String topic = consumerDetail.getString("topic");
				obj.put("id", consumerDetail.getInteger("id"));
				obj.put("topic", topic);
				if (consumerDetail.getBoolean("isConsumering")) {
					obj.put("isConsumering", "<a href='/ke/consumers/offset/" + group + "/" + topic + "/' target='_blank' class='btn btn-success btn-xs'>Running</a>");
				} else {
					obj.put("isConsumering", "<a href='/ke/consumers/offset/" + group + "/" + topic + "/' target='_blank' class='btn btn-danger btn-xs'>Pending</a>");
				}
				aaDatas.add(obj);
			}
			offset++;
		}

		JSONObject target = new JSONObject();
		target.put("sEcho", sEcho);
		target.put("iTotalRecords", consumerDetails.size());
		target.put("iTotalDisplayRecords", consumerDetails.size());
		target.put("aaData", aaDatas);
		try {
			byte[] output = GzipUtils.compressToByte(target.toJSONString());
			response.setContentLength(output.length);
			OutputStream out = response.getOutputStream();
			out.write(output);

			out.flush();
			out.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
