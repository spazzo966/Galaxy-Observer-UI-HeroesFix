// This is an open source non-commercial project. Dear PVS-Studio, please check it.
// PVS-Studio Static Code Analyzer for C, C++ and C#: http://www.viva64.com

package com.ahli.hotkey_ui.application.galaxy.ext;

import com.ahli.hotkey_ui.application.model.ValueDef;
import com.ahli.util.SilentXmlSaxErrorHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LayoutExtensionReader {
	private static final String CONSTANT = "constant";
	private static final String DEFAULT = "default";
	private static final String DESCRIPTION = "description";
	private static final String HOTKEY_SETTING_REGEX = "(?=@hotkey|@setting)/i";
	private static final String HOTKEY = "@hotkey";
	private static final String SETTING = "@setting";
	private static final String CONSTANT_DEFAULT_DESCRIPTION_REGEX = "(?i)(?=(constant|default|description)[\\s]*=)";
	private static final String EMPTY_STRING = "";
	private static final String NAME = "name";
	private static final String VAL = "val";
	private static final String ERROR_PARSING_FILE = "Error parsing file.";
	private static final Logger logger = LogManager.getLogger(LayoutExtensionReader.class);
	private List<ValueDef> hotkeys = new ArrayList<>();
	private List<ValueDef> settings = new ArrayList<>();
	
	public LayoutExtensionReader() {
		// nothing to do
	}
	
	/**
	 * @param nodes
	 * @param name
	 * @return
	 */
	private static Node getNamedItemIgnoreCase(final NamedNodeMap nodes, final String name) {
		final Node node = nodes.getNamedItem(name);
		if (node == null) {
			for (int i = 0, len = nodes.getLength(); i < len; i++) {
				final Node curNode = nodes.item(i);
				if (name.equalsIgnoreCase(curNode.getNodeName())) {
					return curNode;
				}
			}
		}
		return node;
	}
	
	private static String getValueAfterEqualsChar(final String part) {
		return part.substring(1 + part.indexOf('=')).trim();
	}
	
	private static String getValueWithinQuotes(final String part) {
		final int quoteEnd = part.lastIndexOf('"');
		final int quoteStart = part.indexOf('"');
		if (quoteStart < 0 || quoteStart >= quoteEnd) {
			return null;
		}
		return part.substring(quoteStart + 1, quoteEnd);
	}
	
	/**
	 * @return the hotkeys
	 */
	public List<ValueDef> getHotkeys() {
		return hotkeys;
	}
	
	/**
	 * @param hotkeys
	 * 		the hotkeys to set
	 */
	public void setHotkeys(final List<ValueDef> hotkeys) {
		this.hotkeys = hotkeys;
	}
	
	/**
	 * @return the settings
	 */
	public List<ValueDef> getSettings() {
		return settings;
	}
	
	/**
	 * @param settings
	 * 		the settings to set
	 */
	public void setSettings(final List<ValueDef> settings) {
		this.settings = settings;
	}
	
	/**
	 * Processes the specified Layout files. It finds hotkeys and setting definitions.
	 *
	 * @param layoutFiles
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public void processLayoutFiles(final Iterable<File> layoutFiles) throws ParserConfigurationException, SAXException {
		if (logger.isInfoEnabled()) {
			logger.info("Scanning for XML file...");
		}
		
		final DocumentBuilderFactory dbFac = DocumentBuilderFactory.newInstance();
		dbFac.setNamespaceAware(false);
		dbFac.setValidating(false);
		dbFac.setAttribute("http://xml.org/sax/features/external-general-entities", false);
		dbFac.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, EMPTY_STRING);
		dbFac.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		dbFac.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		dbFac.setXIncludeAware(false);
		dbFac.setExpandEntityReferences(false);
		dbFac.setAttribute(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		final DocumentBuilder dBuilder = dbFac.newDocumentBuilder();
		
		// provide error handler that does not print incompatible files into
		// console
		dBuilder.setErrorHandler(new SilentXmlSaxErrorHandler());
		
		Document doc;
		for (final File curFile : layoutFiles) {
			try {
				doc = dBuilder.parse(curFile);
			} catch (final SAXParseException | IOException e) {
				if (logger.isTraceEnabled()) {
					logger.trace(ERROR_PARSING_FILE, e);
				}
				// couldn't parse, most likely no XML file
				continue;
			}
			
			if (logger.isDebugEnabled()) {
				logger.debug("comments - processing file: {}", curFile.getPath());
			}
			
			// read comments
			final Element elem = doc.getDocumentElement();
			final NodeList childNodes = elem.getChildNodes();
			readComments(childNodes);
		}
		
		for (final File curFile : layoutFiles) {
			try {
				// parse XML file
				doc = dBuilder.parse(curFile);
			} catch (final SAXParseException | IOException e) {
				if (logger.isTraceEnabled()) {
					logger.trace(ERROR_PARSING_FILE, e);
				}
				// couldn't parse, most likely no XML file
				continue;
			}
			
			if (logger.isDebugEnabled()) {
				logger.debug("constants - processing file: {}", curFile.getPath());
			}
			
			// read constants
			final Element elem = doc.getDocumentElement();
			final NodeList childNodes = elem.getChildNodes();
			readConstants(childNodes);
		}
	}
	
	/**
	 * Processes the Comments in the given Nodes.
	 *
	 * @param childNodes
	 */
	private void readComments(final NodeList childNodes) {
		for (int i = 0; i < childNodes.getLength(); i++) {
			final Node curNode = childNodes.item(i);
			
			if (curNode.getNodeType() == Node.COMMENT_NODE) {
				
				final Comment comment = (Comment) curNode;
				final String text = comment.getData();
				processCommentText(text);
				
			} else {
				readComments(curNode.getChildNodes());
			}
		}
	}
	
	/**
	 * Creates ValueDef for the Hotkey and Setting definitions found in this comment
	 *
	 * @param textInput
	 */
	public void processCommentText(final String textInput) {
		String constant;
		String description;
		String defaultValue;
		
		logger.debug("textInput:{}", () -> textInput);
		try {
			// split at keywords @hotkey or @setting without removing, case insensitive
			for (String text : textInput.split(HOTKEY_SETTING_REGEX)) {
				if (logger.isDebugEnabled()) {
					logger.debug("token start:{}", text);
				}
				text = text.trim();
				
				constant = EMPTY_STRING;
				description = EMPTY_STRING;
				defaultValue = EMPTY_STRING;
				
				final boolean isHotkey = text.toLowerCase(Locale.ROOT).startsWith(HOTKEY);
				final boolean isSetting = text.toLowerCase(Locale.ROOT).startsWith(SETTING);
				
				if (isHotkey || isSetting) {
					if (logger.isDebugEnabled()) {
						logger.debug("detected hotkey or setting");
					}
					// move behind keyword
					final int pos = isHotkey ? HOTKEY.length() : SETTING.length();
					String toProcess = text.substring(pos);
					
					// move beyond '('
					toProcess = toProcess.substring(1 + toProcess.indexOf('(')).trim();
					
					// split at keyword
					for (String part : toProcess.split(CONSTANT_DEFAULT_DESCRIPTION_REGEX)) {
						part = part.trim();
						final String partLower = part.toLowerCase(Locale.ROOT);
						if (logger.isTraceEnabled()) {
							logger.trace("part: {}", part);
						}
						if (partLower.startsWith(CONSTANT)) {
							// move beyond '='
							part = getValueAfterEqualsChar(part);
							constant = getValueWithinQuotes(part);
							if (logger.isTraceEnabled()) {
								logger.trace("constant = {}", constant);
							}
						} else if (partLower.startsWith(DEFAULT)) {
							// move beyond '='
							part = getValueAfterEqualsChar(part);
							defaultValue = getValueWithinQuotes(part);
							if (logger.isTraceEnabled()) {
								logger.trace("default = {}", defaultValue);
							}
						} else if (partLower.startsWith(DESCRIPTION)) {
							// move beyond '='
							part = getValueAfterEqualsChar(part);
							description = getValueWithinQuotes(part);
							if (logger.isTraceEnabled()) {
								logger.trace("description = {}", description);
							}
						}
					}
					
					if (constant != null && !EMPTY_STRING.equals(constant)) {
						if (isHotkey) {
							addHotkeyValueDef(constant, description, defaultValue, EMPTY_STRING);
						} else {
							addSettingValueDef(constant, description, defaultValue, EMPTY_STRING);
						}
					}
				}
			}
		} catch (final RuntimeException e) {
			throw e;
		} catch (final Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("Parsing Comment failed.", e);
			}
		}
	}
	
	public void addHotkeyValueDef(final String constant, final String description, final String defaultValue,
			final String curValue) {
		final ValueDef def = new ValueDef(constant, curValue, description, defaultValue);
		hotkeys.add(def);
	}
	
	public void addSettingValueDef(final String constant, final String description, final String defaultValue,
			final String curValue) {
		final ValueDef def = new ValueDef(constant, curValue, description, defaultValue);
		settings.add(def);
	}
	
	/**
	 * Processes the Constants in the given Nodes.
	 *
	 * @param childNodes
	 */
	private void readConstants(final NodeList childNodes) {
		for (int i = 0; i < childNodes.getLength(); i++) {
			final Node curNode = childNodes.item(i);
			
			if (curNode.getNodeType() == Node.ELEMENT_NODE) {
				
				final String nodeName = curNode.getNodeName().toLowerCase(Locale.ROOT);
				if (nodeName.equals(CONSTANT)) {
					processConstant(curNode);
				}
				
			} else {
				readConstants(curNode.getChildNodes());
			}
		}
	}
	
	/**
	 * @param node
	 */
	public void processConstant(final Node node) {
		final Node nameAttrNode = getNamedItemIgnoreCase(node.getAttributes(), NAME);
		if (nameAttrNode != null) {
			final String name = nameAttrNode.getNodeValue();
			final Node valAttrNode = getNamedItemIgnoreCase(node.getAttributes(), VAL);
			if (valAttrNode != null) {
				final String val = valAttrNode.getNodeValue();
				if (logger.isDebugEnabled()) {
					logger.debug("Constant: name = {}, val = {}", name, val);
				}
				setValueDefCurValue(name, val);
			} else {
				if (logger.isWarnEnabled()) {
					logger.warn("Constant has no 'val' attribute defined.");
				}
			}
		} else {
			if (logger.isWarnEnabled()) {
				logger.warn("Constant has no 'name' attribute defined.");
			}
		}
	}
	
	/**
	 * @param name
	 * @param val
	 */
	private void setValueDefCurValue(final String name, final String val) {
		for (final ValueDef item : hotkeys) {
			if (item.getId().equalsIgnoreCase(name)) {
				item.setValue(val);
				return;
			}
		}
		for (final ValueDef item : settings) {
			if (item.getId().equalsIgnoreCase(name)) {
				item.setValue(val);
				return;
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("no ValueDef found with name: {}", name);
		}
	}
	
	/**
	 * @param layoutFiles
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws SAXException
	 */
	public void updateLayoutFiles(final Iterable<File> layoutFiles) throws ParserConfigurationException, SAXException {
		if (logger.isInfoEnabled()) {
			logger.info("Scanning for XML file...");
		}
		final DocumentBuilderFactory dbFac = DocumentBuilderFactory.newInstance();
		dbFac.setNamespaceAware(false);
		dbFac.setValidating(false);
		dbFac.setAttribute("http://xml.org/sax/features/external-general-entities", false);
		dbFac.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, EMPTY_STRING);
		dbFac.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		dbFac.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		dbFac.setXIncludeAware(false);
		dbFac.setExpandEntityReferences(false);
		dbFac.setAttribute(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		final DocumentBuilder dBuilder = dbFac.newDocumentBuilder();
		// provide error handler that does not print incompatible files into console
		dBuilder.setErrorHandler(new SilentXmlSaxErrorHandler());
		
		Document doc;
		final Transformer transformer;
		try {
			final TransformerFactory factory = TransformerFactory.newInstance();
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			transformer = factory.newTransformer();
			
			for (final File curFile : layoutFiles) {
				try {
					// parse XML file
					doc = dBuilder.parse(curFile);
				} catch (final SAXParseException | IOException e) {
					if (logger.isTraceEnabled()) {
						logger.trace(ERROR_PARSING_FILE, e);
					}
					continue;
				}
				
				if (logger.isDebugEnabled()) {
					logger.debug("processing file: {}", curFile.getPath());
				}
				
				// process files
				final Element elem = doc.getDocumentElement();
				final NodeList childNodes = elem.getChildNodes();
				modifyConstants(childNodes);
				
				// write DOM back to XML
				try {
					transformer.transform(new DOMSource(doc), new StreamResult(curFile));
				} catch (final TransformerException e) {
					logger.error("Transforming to generate XML file failed.", e);
				}
			}
		} catch (final TransformerConfigurationException e) {
			logger.error("Transforming to generate XML file failed.", e);
		}
	}
	
	private void modifyConstants(final NodeList childNodes) {
		for (int i = 0; i < childNodes.getLength(); i++) {
			final Node curNode = childNodes.item(i);
			
			if (curNode.getNodeType() == Node.ELEMENT_NODE) {
				
				final String nodeName = curNode.getNodeName().toLowerCase(Locale.ROOT);
				if (nodeName.equals(CONSTANT)) {
					modifyConstant(curNode);
				}
				
			} else {
				readConstants(curNode.getChildNodes());
			}
		}
	}
	
	/**
	 * Modify a Constant node from XML with data's current value.
	 *
	 * @param node
	 */
	private void modifyConstant(final Node node) {
		final Node nameAttrNode = getNamedItemIgnoreCase(node.getAttributes(), NAME);
		if (nameAttrNode != null) {
			final String name = nameAttrNode.getNodeValue();
			final Node valAttrNode = getNamedItemIgnoreCase(node.getAttributes(), VAL);
			if (valAttrNode != null) {
				final String val = valAttrNode.getNodeValue();
				
				for (final ValueDef item : hotkeys) {
					if (item.getId().equalsIgnoreCase(name)) {
						if (logger.isDebugEnabled()) {
							logger.debug("updating hotkey constant: {}, with val: {}", name, val);
						}
						valAttrNode.setNodeValue(item.getValue());
					}
				}
				for (final ValueDef item : settings) {
					if (item.getId().equalsIgnoreCase(name)) {
						if (logger.isDebugEnabled()) {
							logger.debug("updating setting constant: {}, with val: {}", name, val);
						}
						valAttrNode.setNodeValue(item.getValue());
					}
				}
			} else {
				if (logger.isWarnEnabled()) {
					logger.warn("Constant has no 'val' attribute defined.");
				}
			}
		} else {
			if (logger.isWarnEnabled()) {
				logger.warn("Constant has no 'name' attribute defined.");
			}
		}
	}
	
	/**
	 * Clears stored data.
	 */
	public void clearData() {
		hotkeys.clear();
		settings.clear();
	}
}
