package ilg.gnuarmeclipse.packs;

import ilg.gnuarmeclipse.packs.TreeNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class PdscParser {

	private MessageConsoleStream m_out;
	private boolean m_isBrief;

	private IPath m_path;
	private Document m_document;

	public PdscParser() {

		MessageConsole myConsole = Utils.findConsole();
		m_out = myConsole.newMessageStream();

		m_isBrief = false;
	}

	public void setIsBrief(boolean brief) {
		m_isBrief = brief;
	}

	public boolean isBrief() {
		return m_isBrief;
	}

	private String extendDescription(String description, String value) {
		return extendDescription(description, null, value);
	}

	private String extendDescription(String description, String comment,
			String value) {

		if (value.length() > 0) {
			if (description.length() > 0)
				description += "\n";
			if (comment != null && comment.length() > 0) {
				description += comment + ": ";
			}
			description += value;
		}
		return description;
	}

	private String extendName(String name, String value) {

		if (value.length() > 0) {
			if (name.length() > 0)
				name += " / ";
			name += value;
		}
		return name;
	}

	private String updatePosixSeparators(String spath) {
		return spath.replace('\\', '/');
	}

	public Document parseXml(IPath path) throws ParserConfigurationException,
			SAXException, IOException {

		File file = path.toFile();
		if (file == null) {
			throw new FileNotFoundException(path.toFile().toString());
		}

		m_path = path;
		InputSource inputSource = new InputSource(new FileInputStream(file));

		DocumentBuilder xml = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder();
		m_document = xml.parse(inputSource);

		return m_document;
	}

	public TreeNode parsePdscFull() throws ParserConfigurationException,
			SAXException, IOException {

		TreeNode tree = new TreeNode(TreeNode.OUTLINE_TYPE);
		tree.putProperty(TreeNode.FOLDER_PROPERTY, m_path.removeLastSegments(1)
				.toString());

		Element packageElement = m_document.getDocumentElement();
		String firstElementName = packageElement.getNodeName();
		if (!"package".equals(firstElementName)) {
			System.out.println("Missing <packages>, <" + firstElementName
					+ "> encountered");
			return tree;
		}

		String schemaVersion = packageElement.getAttribute("schemaVersion")
				.trim();

		if ("1.0".equals(schemaVersion)) {
			;
		} else if ("1.1".equals(schemaVersion)) {
			;
		} else if ("1.2".equals(schemaVersion)) {
			;
		} else {
			System.out.println("Unrecognised schema version " + schemaVersion);
			return tree;
		}

		TreeNode packNode = new TreeNode(TreeNode.PACKAGE_TYPE);
		tree.addChild(packNode);

		String packDescription = "";

		List<Element> childElements = Utils
				.getChildElementsList(packageElement);
		for (Element childElement : childElements) {

			String elementName = childElement.getNodeName();
			if ("vendor".equals(elementName)) {

				String vendorName = Utils.getElementContent(childElement);
				packNode.putProperty(TreeNode.VENDOR_PROPERTY, vendorName);

			} else if ("name".equals(elementName)) {

				String packName = Utils.getElementContent(childElement);
				packNode.setName(packName);

				packDescription = "Package: " + packName;

			} else if ("description".equals(elementName)) {

				// Warning: must be located after <name>
				packDescription = extendDescription(packDescription,
						Utils.getElementContent(childElement));

			} else if ("url".equals(elementName)) {

				String url = Utils.getElementContent(childElement);
				packNode.putNonEmptyProperty(TreeNode.URL_PROPERTY, url);

				packDescription = extendDescription(packDescription, "url", url);

			} else if ("releases".equals(elementName)) {

				List<Element> releaseElements = Utils.getChildElementsList(
						childElement, "release");
				if (!releaseElements.isEmpty()) {
					Element releaseElement = releaseElements.get(0);

					String releaseVersion = releaseElement.getAttribute(
							"version").trim();
					// Optional
					String releaseDate = releaseElement.getAttribute("date")
							.trim();

					String description;
					description = "Version: " + releaseVersion;
					if (releaseDate.length() > 0) {
						description += ", from " + releaseDate;
					}
					description = extendDescription(description,
							Utils.getElementContent(releaseElement));

					TreeNode versionNode = new TreeNode(TreeNode.VERSION_TYPE);
					versionNode.setName(releaseVersion);
					versionNode.setDescription(description);
					versionNode.putNonEmptyProperty(TreeNode.DATE_PROPERTY,
							releaseDate);

					tree.addChild(versionNode);
				}

			} else if ("keywords".equals(elementName)) {

				List<Element> childElements2 = Utils
						.getChildElementsList(childElement);
				for (Element childElement2 : childElements2) {

					String elementName2 = childElement2.getNodeName();
					if ("keyword".equals(elementName2)) {

						processKeyword(childElement2, tree);

					} else {
						System.out.println("Not processed <" + elementName2
								+ ">");
					}
				}

			} else if ("devices".equals(elementName)) {

				// TreeNode devicesNode = new TreeNode(TreeNode.DEVICES_TYPE);
				// tree.addChild(devicesNode);

				List<Element> childElements2 = Utils
						.getChildElementsList(childElement);
				for (Element childElement2 : childElements2) {

					String elementName2 = childElement2.getNodeName();
					if ("family".equals(elementName2)) {

						processFamily(childElement2, tree);

					} else {
						System.out.println("Not processed <" + elementName2
								+ ">");
					}
				}

			} else if ("boards".equals(elementName)) {

				// TreeNode boardsNode = new TreeNode(TreeNode.BOARDS_TYPE);
				// tree.addChild(boardsNode);

				List<Element> childElements2 = Utils
						.getChildElementsList(childElement);
				for (Element childElement2 : childElements2) {

					String elementName2 = childElement2.getNodeName();
					if ("board".equals(elementName2)) {

						processBoard(childElement2, tree);

					} else {
						System.out.println("Not processed <" + elementName2
								+ ">");
					}
				}

			} else if ("conditions".equals(elementName)) {

				List<Element> childElements2 = Utils
						.getChildElementsList(childElement);
				for (Element childElement2 : childElements2) {

					String elementName2 = childElement2.getNodeName();
					if ("condition".equals(elementName2)) {

						processCondition(childElement2, tree);

					} else {
						System.out.println("Not processed <" + elementName2
								+ ">");
					}
				}

			} else if ("examples".equals(elementName)) {

				// TreeNode examplesNode = new TreeNode(TreeNode.EXAMPLES_TYPE);
				// tree.addChild(examplesNode);

				List<Element> childElements2 = Utils
						.getChildElementsList(childElement);
				for (Element childElement2 : childElements2) {

					String elementName2 = childElement2.getNodeName();
					if ("example".equals(elementName2)) {

						processExample(childElement2, tree, false);

					} else {
						System.out.println("Not processed <" + elementName2
								+ ">");
					}
				}

			} else if ("components".equals(elementName)) {

				// TreeNode componentsNode = new
				// TreeNode(TreeNode.COMPONENTS_TYPE);
				// tree.addChild(componentsNode);

				List<Element> childElements2 = Utils
						.getChildElementsList(childElement);
				for (Element childElement2 : childElements2) {

					String elementName2 = childElement2.getNodeName();
					if ("component".equals(elementName2)) {

						processComponent(childElement2, tree, "");

					} else if ("bundle".equals(elementName2)) {

						processBundle(childElement2, tree);

					} else {
						System.out.println("Not processed <" + elementName2
								+ ">");
					}
				}

			} else if ("apis".equals(elementName)) {

				// <xs:complexType name="ApisType">
				// <xs:sequence>
				// <xs:element name="api" type="ApiType" minOccurs="1"
				// maxOccurs="unbounded"/>
				// </xs:sequence>
				// </xs:complexType>

				List<Element> childElements2 = Utils
						.getChildElementsList(childElement);
				for (Element childElement2 : childElements2) {

					String elementName2 = childElement2.getNodeName();
					if ("api".equals(elementName2)) {

						processApi(childElement2, tree);

					} else {
						System.out.println("Not processed <" + elementName2
								+ ">");
					}
				}

			} else if ("taxonomy".equals(elementName)) {

				List<Element> childElements2 = Utils
						.getChildElementsList(childElement);
				for (Element childElement2 : childElements2) {

					String elementName2 = childElement2.getNodeName();
					if ("description".equals(elementName2)) {

						processTaxonomyDescription(childElement2, tree);

					} else {
						System.out.println("Not processed <" + elementName2
								+ ">");
					}
				}
			} else if ("generators".equals(elementName)) {

				// Ignore

			} else {
				System.out.println("Not processed <" + elementName + ">");
			}
		}

		packDescription = extendDescription(packDescription, "schema",
				schemaVersion);
		packNode.setDescription(packDescription);

		return tree;
	}

	// <!-- Family Level begin -->
	// <xs:element name="family" maxOccurs="unbounded">
	// <xs:complexType>
	// <xs:sequence>
	// <xs:group ref="DevicePropertiesGroup"/>
	// <xs:element name="device" type="DeviceType" minOccurs="0"
	// maxOccurs="unbounded"/>
	// <!-- Sub Family Level begin-->
	// <xs:element name="subFamily" minOccurs="0" maxOccurs="unbounded">
	// <xs:complexType>
	// <xs:sequence>
	// <xs:group ref="DevicePropertiesGroup"/>
	// <!-- Device Level begin-->
	// <xs:element name="device" type="DeviceType" maxOccurs="unbounded"/>
	// <!-- Device Level end -->
	// </xs:sequence>
	// <xs:attribute name="DsubFamily" type="xs:string" use="required"/>
	// </xs:complexType>
	// </xs:element>
	// <!-- Sub Family Level end -->
	// </xs:sequence>
	// <xs:attribute name="Dfamily" type="xs:string" use="required"/>
	// <xs:attribute name="Dvendor" type="DeviceVendorEnum" use="required"/>
	// </xs:complexType>
	// </xs:element>
	// <!-- Family Level end -->

	private void processFamily(Element el, TreeNode parent) {

		// Required
		String familyName = el.getAttribute("Dfamily").trim();
		String familyVendor = el.getAttribute("Dvendor").trim();

		String va[] = familyVendor.split("[:]");

		TreeNode familyNode = new TreeNode(TreeNode.FAMILY_TYPE);
		parent.addChild(familyNode);

		familyNode.setName(familyName + " (family)");

		familyNode.putNonEmptyProperty(TreeNode.VENDOR_PROPERTY, va[0]);
		familyNode.putNonEmptyProperty(TreeNode.VENDORID_PROPERTY, va[1]);

		String familyDescription = "Device family: " + familyName;

		List<Element> childElements = Utils.getChildElementsList(el);

		for (Element childElement : childElements) {

			String elementName = childElement.getNodeName();

			if (isBrief()) {
				if ("description".equals(elementName)) {

					familyDescription = extendDescription(familyDescription,
							Utils.getElementContent(childElement));

				}
			} else {
				if ("processor".equals(elementName)) {

					processProcessor(childElement, familyNode);

				} else if ("book".equals(elementName)) {

					processBook(childElement, familyNode);

				} else if ("description".equals(elementName)) {

					familyDescription = extendDescription(familyDescription,
							Utils.getElementContent(childElement));

				} else if ("feature".equals(elementName)) {

					processFeature(childElement, familyNode);

				} else if ("subFamily".equals(elementName)) {

					processSubFamily(childElement, familyNode);

				} else if ("device".equals(elementName)) {

					processDevice(childElement, familyNode);

				} else {
					System.out.println("Not processed <" + elementName + ">");
				}
			}
		}

		familyNode.setDescription(familyDescription);
	}

	// <!-- Processor Type -->
	// <xs:complexType name="ProcessorType">
	// <!-- Pname defines an identifier for a specific processor in a
	// multi-processor devices -->
	// <xs:attribute name="Pname" type="xs:string"/>
	// <!-- Dcore specifies the processor from a list of supported processors
	// -->
	// <xs:attribute name="Dcore" type="DcoreEnum"/>
	// <!-- Dfpu specifies the hardware floating point unit -->
	// <xs:attribute name="Dfpu" type="DfpuEnum"/>
	// <!-- Dmpu specifies the memory protection unit -->
	// <xs:attribute name="Dmpu" type="DmpuEnum"/>
	// <!-- Dendian specifies the endianess supported by the processor -->
	// <xs:attribute name="Dendian" type="DendianEnum"/>
	// <!-- Dclock specifies the maximum core clock frequency -->
	// <xs:attribute name="Dclock" type="xs:unsignedInt"/>
	// <!-- DcoreVersion specifies the revision of the processor -->
	// <xs:attribute name="DcoreVersion" type="xs:string"/>
	// </xs:complexType>

	private void processProcessor(Element el, TreeNode parent) {

		String Dcore = el.getAttribute("Dcore").trim();
		String DcoreVersion = el.getAttribute("DcoreVersion").trim();
		String Dfpu = el.getAttribute("Dfpu").trim();
		String Dmpu = el.getAttribute("Dmpu").trim();
		String Dendian = el.getAttribute("Dendian").trim();
		String Dclock = el.getAttribute("Dclock").trim();

		TreeNode processorNode = new TreeNode(TreeNode.PROCESSOR_TYPE);
		parent.addChild(processorNode);

		String name;
		if (Dcore.length() > 0) {
			name = Dcore;
		} else if (Dclock.length() > 0) {
			name = Dclock;
		} else {
			name = "Processor";
		}
		processorNode.setName(name);

		processorNode.putNonEmptyProperty(TreeNode.VERSION_PROPERTY,
				DcoreVersion);
		processorNode.putNonEmptyProperty(TreeNode.FPU_PROPERTY, Dfpu);
		processorNode.putNonEmptyProperty(TreeNode.MPU_PROPERTY, Dmpu);
		processorNode.putNonEmptyProperty(TreeNode.ENDIAN_PROPERTY, Dendian);

		String description = "Processor";
		description = extendDescription(description, "Dcore", Dcore);
		description = extendDescription(description, "DcoreVersion",
				DcoreVersion);
		description = extendDescription(description, "Dfpu", Dfpu);
		description = extendDescription(description, "Dmpu", Dmpu);
		description = extendDescription(description, "Dendian", Dendian);
		description = extendDescription(description, "Dclock", Dclock);

		processorNode.setDescription(description);
	}

	// <!-- Board Feature Type -->
	// <xs:complexType name="BoardFeatureType">
	// <xs:attribute name="type" type="BoardFeatureTypeEnum" use="required"/>
	// <xs:attribute name="n" type="xs:decimal" use="optional"/>
	// <xs:attribute name="m" type="xs:decimal" use="optional"/>
	// <xs:attribute name="name" type="xs:string" use="optional"/>
	// </xs:complexType>

	// <!-- Device Feature Type -->
	// <xs:complexType name="DeviceFeatureType">
	// <xs:attribute name="Pname" type="xs:string" use="optional"/>
	// <xs:attribute name="type" type="DeviceFeatureTypeEnum" use="required"/>
	// <xs:attribute name="n" type="xs:decimal" use="optional"/>
	// <xs:attribute name="m" type="xs:decimal" use="optional"/>
	// <xs:attribute name="name" type="xs:string" use="optional"/>
	// <!-- deprecated, only for backwards compatibility -->
	// <xs:attribute name="count" type="xs:int" use="optional"/>
	// </xs:complexType>

	private void processFeature(Element el, TreeNode parent) {

		// Required
		String featureType = el.getAttribute("type").trim();

		// Optional
		String featureN = el.getAttribute("n").trim();
		String featureM = el.getAttribute("m").trim();
		String name = el.getAttribute("name").trim();
		String Pname = el.getAttribute("Pname").trim();

		TreeNode featureNode = new TreeNode(TreeNode.FEATURE_TYPE);
		parent.addChild(featureNode);

		featureNode.setName(featureType);
		featureNode.putNonEmptyProperty(TreeNode.N_PROPERTY, featureN);
		featureNode.putNonEmptyProperty(TreeNode.M_PROPERTY, featureM);

		String description = "Feature";
		if (name.length() > 0) {
			description += ": " + name;
		}
		description = extendDescription(description, "type", featureType);
		description = extendDescription(description, "n", featureN);
		description = extendDescription(description, "m", featureM);
		description = extendDescription(description, "Pname", Pname);

		featureNode.setDescription(description);
	}

	// <xs:complexType name="BoardsBookType">
	// <xs:attribute name="category" type="BoardBookCategoryEnum"/>
	// <xs:attribute name="name" type="xs:string"/>
	// <xs:attribute name="title" type="xs:string"/>
	// </xs:complexType>

	private void processBook(Element el, TreeNode parent) {

		String bookName = el.getAttribute("name").trim();

		String bookTitle = el.getAttribute("title").trim();
		String category = el.getAttribute("category").trim();

		TreeNode bookNode = new TreeNode(TreeNode.BOOK_TYPE);
		parent.addChild(bookNode);

		bookNode.setName(bookTitle);

		String description = "Book: " + bookTitle;

		String posixName = updatePosixSeparators(bookName);

		if (bookName.startsWith("http://") || bookName.startsWith("https://")
				|| bookName.startsWith("ftp://")) {
			bookNode.putNonEmptyProperty(TreeNode.URL_PROPERTY, bookName);
			description = extendDescription(description, "url", bookName);
		} else {
			bookNode.putNonEmptyProperty(TreeNode.FILE_PROPERTY, posixName);
			description = extendDescription(description, "file", posixName);
		}

		bookNode.putNonEmptyProperty(TreeNode.CATEGORY_PROPERTY, category);

		description = extendDescription(description, "category", category);

		bookNode.setDescription(description);

	}

	// <xs:element name="subFamily" minOccurs="0" maxOccurs="unbounded">
	// <xs:complexType>
	// <xs:sequence>
	// <xs:group ref="DevicePropertiesGroup"/>
	// <!-- Device Level begin-->
	// <xs:element name="device" type="DeviceType" maxOccurs="unbounded"/>
	// <!-- Device Level end -->
	// </xs:sequence>
	// <xs:attribute name="DsubFamily" type="xs:string" use="required"/>
	// </xs:complexType>
	// </xs:element>

	private void processSubFamily(Element el, TreeNode parent) {

		String subFamilyName = el.getAttribute("DsubFamily").trim();

		TreeNode subFamilyNode = new TreeNode(TreeNode.SUBFAMILY_TYPE);
		parent.addChild(subFamilyNode);

		subFamilyNode.setName(subFamilyName + " (subfamily)");

		String description = "Device subfamily: " + subFamilyName;

		List<Element> childElements = Utils.getChildElementsList(el);

		for (Element childElement : childElements) {

			String elementName = childElement.getNodeName();
			if ("description".equals(elementName)) {

				description = extendDescription(description,
						Utils.getElementContent(childElement));

			} else if ("device".equals(elementName)) {

				processDevice(childElement, subFamilyNode);

			} else {

				processDevicePropertiesGroup(childElement, subFamilyNode);

			}
		}

		subFamilyNode.setDescription(description);
	}

	// <xs:complexType name="DeviceType">
	// <xs:sequence>
	// <xs:group ref="DevicePropertiesGroup"/>
	// <!-- Variant Level begin-->
	// <xs:element name="variant" minOccurs="0" maxOccurs="unbounded">
	// <xs:complexType>
	// <xs:sequence>
	// <xs:group ref="DevicePropertiesGroup"/>
	// </xs:sequence>
	// <xs:attribute name="Dvariant" type="xs:string" use="required"/>
	// </xs:complexType>
	// </xs:element>
	// <!-- Variant Level end -->
	// </xs:sequence>
	// <xs:attribute name="Dname" type="xs:string" use="required"/>
	// <!-- <xs:attributeGroup ref="DefaultDeviceAttributesGroup"/> -->
	// </xs:complexType>

	private void processDevice(Element el, TreeNode parent) {

		// Required
		String deviceName = el.getAttribute("Dname").trim();

		TreeNode deviceNode = new TreeNode(TreeNode.DEVICE_TYPE);
		parent.addChild(deviceNode);

		deviceNode.setName(deviceName + " (device)");

		String descriptionContent = "";

		List<Element> childElements = Utils.getChildElementsList(el);

		for (Element childElement : childElements) {

			String elementName = el.getNodeName();
			if ("description".equals(elementName)) {

				descriptionContent = Utils.getElementContent(childElement);

			} else if ("variant".equals(elementName)) {

				// Required
				String variantName = el.getAttribute("Dvariant").trim();

				TreeNode variantNode = new TreeNode(TreeNode.VARIANT_TYPE);
				deviceNode.addChild(variantNode);

				variantNode.setName(variantName + " (variant)");

				String variantDescriptionContent = "Device variant: "
						+ variantName;

				List<Element> childElements2 = Utils
						.getChildElementsList(childElement);
				for (Element childElement2 : childElements2) {

					String elementName2 = childElement2.getNodeName();
					if ("description".equals(elementName2)) {

						variantDescriptionContent = extendDescription(
								variantDescriptionContent,
								Utils.getElementContent(childElement));
					} else {
						processDevicePropertiesGroup(childElement2, variantNode);
					}
				}

				variantNode.setDescription(variantDescriptionContent);

			} else {

				processDevicePropertiesGroup(childElement, deviceNode);
			}
		}

		String deviceDescription = "Device: " + deviceName;

		int ramKB = 0;
		int romKB = 0;

		for (TreeNode childNode : deviceNode.getChildrenArray()) {

			if (TreeNode.MEMORY_TYPE.equals(childNode.getType())) {
				String size = childNode.getProperty(TreeNode.SIZE_PROPERTY, "");
				int sizeKB = Utils.convertHexInt(size) / 1024;

				String id = childNode.getProperty(TreeNode.ID_PROPERTY, "");
				if (id.contains("ROM")) {
					romKB += sizeKB;
				} else if (id.contains("RAM")) {
					ramKB += sizeKB;
				} else {
				}
			}
		}

		if (ramKB > 0) {
			if (deviceDescription.length() > 0) {
				deviceDescription += ", ";
			}

			deviceDescription += String.valueOf(ramKB) + " kB RAM";
		}

		if (romKB > 0) {
			if (deviceDescription.length() > 0) {
				deviceDescription += ", ";
			}

			deviceDescription += String.valueOf(romKB) + " kB ROM";
		}

		deviceDescription = extendDescription(deviceDescription,
				descriptionContent);

		deviceNode.setDescription(deviceDescription);
	}

	// <!-- Device Properties Group -->
	// <xs:group name="DevicePropertiesGroup">
	// <!-- multi-core devices have unique Pname attribute. One entry per
	// processor and level -->
	// <xs:sequence>
	// <xs:element name="processor" type="ProcessorType" minOccurs="0"
	// maxOccurs="unbounded"/>
	// <xs:element name="jtagconfig" type="JtagConfigType" minOccurs="0"
	// maxOccurs="1"/>
	// <xs:element name="swdconfig" type="SwdConfigType" minOccurs="0"
	// maxOccurs="1"/>
	// <xs:group ref="DefaultDevicePropertiesGroup" minOccurs="0"
	// maxOccurs="unbounded"/>
	// </xs:sequence>
	// </xs:group>

	// <!-- Default Device Properties Group -->
	// <xs:group name="DefaultDevicePropertiesGroup">
	// <!-- multi-core devices have unique Pname attribute. One entry per
	// processor and level -->
	// <xs:choice>
	// <xs:element name="compile" type="CompileType" />
	// <xs:element name="memory" type="MemoryType" />
	// <xs:element name="algorithm" type="AlgorithmType" />
	// <xs:element name="book" type="BookType" />
	// <xs:element name="description" type="DescriptionType" />
	// <xs:element name="feature" type="DeviceFeatureType" />
	// <xs:element name="debugport" type="DebugPortType" />
	// <xs:element name="debug" type="DebugType" />
	// <xs:element name="trace" type="TraceType" />
	// <xs:element name="environment" type="EnvironmentType" />
	// </xs:choice>
	// </xs:group>

	private void processDevicePropertiesGroup(Element el, TreeNode parent) {

		String elementName = el.getNodeName();
		if ("processor".equals(elementName)) {

			processProcessor(el, parent);

			// } else if ("jtagconfig".equals(elementName)) {
			// } else if ("swdconfig".equals(elementName)) {
		} else if ("compile".equals(elementName)) {

			// <!-- Compile Type: -->
			// <xs:complexType name="CompileType">
			// <!-- Pname identifies the processor this setting belongs to -->
			// <xs:attribute name="Pname" type="xs:string" use="optional"/>
			// <!-- CMSIS-CORE device header file (sets compiler include path)
			// -->
			// <xs:attribute name="header" type="xs:string"/>
			// <!-- Device specific preprocessor define (sets preprocessor
			// define -->
			// <xs:attribute name="define" type="xs:string"/>
			// </xs:complexType>

			String header = el.getAttribute("header").trim();
			String define = el.getAttribute("define").trim();

			String Pname = el.getAttribute("Pname").trim();
			if (Pname.length() > 0) {
				System.out.println("Pname not processed " + Pname + "");
			}

			TreeNode headerNode = new TreeNode(TreeNode.HEADER_TYPE);
			parent.addChild(headerNode);

			String posixHeader = updatePosixSeparators(header);
			String va[] = posixHeader.split("/");

			headerNode.setName(va[va.length - 1]);
			headerNode.setDescription("Header file: " + posixHeader);
			headerNode.putProperty(TreeNode.FILE_PROPERTY, posixHeader);

			TreeNode defineNode = new TreeNode(TreeNode.DEFINE_TYPE);
			parent.addChild(defineNode);

			defineNode.setName(define);
			defineNode.setDescription("Macro definition");

		} else if ("memory".equals(elementName)) {

			// <!-- Memory Type-->
			// <xs:complexType name="MemoryType">
			// <!-- Pname identifies the processor this setting belongs to -->
			// <xs:attribute name="Pname" type="xs:string"/>
			// <!-- id specifies the enumerated ID of memory -->
			// <xs:attribute name="id" type="MemoryIDTypeEnum" use="required"/>
			// <!-- start specifies the base address of the memory -->
			// <xs:attribute name="start" type="NonNegativeInteger"
			// use="required"/>
			// <!-- size specifies the size of the memory -->
			// <xs:attribute name="size" type="NonNegativeInteger"
			// use="required"/>
			// <!-- specifies whether the memory shall be initialized -->
			// <xs:attribute name="init" type="xs:boolean" use="optional"
			// default="0"/>
			// <!-- specifies whether the memory is used as default by linker
			// -->
			// <xs:attribute name="default" type="xs:boolean" use="optional"
			// default="0"/>
			// <!-- specifies whether the memory shall be used for the startup
			// by linker -->
			// <xs:attribute name="startup" type="xs:boolean" use="optional"
			// default="0"/>
			// </xs:complexType>

			// Required
			String id = el.getAttribute("id").trim();
			String start = el.getAttribute("start").trim();
			String size = el.getAttribute("size").trim();

			// -
			String Pname = el.getAttribute("Pname").trim();

			// Optional
			String startup = el.getAttribute("startup").trim();
			String init = el.getAttribute("init").trim();
			String defa = el.getAttribute("default").trim();

			TreeNode memoryNode = new TreeNode(TreeNode.MEMORY_TYPE);
			memoryNode.setName(id);

			String description;
			if (id.contains("ROM")) {
				description = "ROM area";
			} else if (id.contains("RAM")) {
				description = "RAM area";
			} else {
				description = "Memory";
			}

			description = extendDescription(description, "id", id);
			description = extendDescription(description, "start", start);
			description = extendDescription(description, "size", size);
			description = extendDescription(description, "startup", startup);
			description = extendDescription(description, "init", init);
			description = extendDescription(description, "default", defa);
			description = extendDescription(description, "Pname", Pname);
			memoryNode.setDescription(description);

			memoryNode.putProperty(TreeNode.ID_PROPERTY, id);
			memoryNode.putProperty(TreeNode.START_PROPERTY, start);
			memoryNode.putProperty(TreeNode.SIZE_PROPERTY, size);

			parent.addChild(memoryNode);

		} else if ("algorithm".equals(elementName)) {

			// Ignore

		} else if ("book".equals(elementName)) {

			processBook(el, parent);

		} else if ("feature".equals(elementName)) {

			processFeature(el, parent);

			// } else if ("debugport".equals(elementName)) {

		} else if ("debug".equals(elementName)) {

			// <!-- DebugType -->
			// <xs:complexType name="DebugType">
			// <xs:sequence>
			// <xs:element name="datapatch" type="DataPatchType" minOccurs="0"
			// maxOccurs="unbounded"/>
			// <xs:element name="sequence" type="SequenceType" minOccurs="0"
			// maxOccurs="unbounded"/>
			// </xs:sequence>
			// <xs:attribute name="Pname" type="xs:string" use="optional"/>
			// <xs:attribute name="dp" type="xs:string" use="optional"/>
			// <xs:attribute name="ap" type="xs:unsignedInt" use="optional"/>
			// <!-- access port index -->
			// <xs:attribute name="svd" type="xs:string" use="optional"/>
			// </xs:complexType>

			String Pname = el.getAttribute("Pname").trim();
			String dp = el.getAttribute("dp").trim();
			String ap = el.getAttribute("ap").trim();
			String svd = el.getAttribute("svd").trim();

			String posixSvd = updatePosixSeparators(svd);
			String va[] = posixSvd.split("/");

			TreeNode debugNode = new TreeNode(TreeNode.DEBUG_TYPE);
			parent.addChild(debugNode);

			debugNode.setName(va[va.length - 1]);

			String description = "Debug";
			description = extendDescription(description, "Pname", Pname);
			description = extendDescription(description, "dp", dp);
			description = extendDescription(description, "ap", ap);
			description = extendDescription(description, "svd", posixSvd);

			debugNode.setDescription(description);
			debugNode.putProperty(TreeNode.FILE_PROPERTY, posixSvd);

			// } else if ("trace".equals(elementName)) {

			// } else if ("environment".equals(elementName)) {

		} else {
			System.out.println("Not processed <" + elementName + ">");
		}
	}

	// <xs:complexType name="BoardType">
	// <xs:sequence>
	// <xs:group ref="BoardElementsGroup" minOccurs="0" maxOccurs="unbounded"/>
	// </xs:sequence>
	// <xs:attribute name="vendor" type="xs:string" use="required"/>
	// <xs:attribute name="name" type="xs:string" use="required"/>
	// <xs:attribute name="revision" type="xs:string" use="optional"/>
	// <xs:attribute name="salesContact" type="xs:string" use="optional"/>
	// <xs:attribute name="orderForm" type="xs:anyURI" use="optional"/>
	// </xs:complexType>
	//
	// <xs:group name="BoardElementsGroup">
	// <xs:choice>
	// <xs:element name="description" type="xs:string"/>
	// <xs:element name="feature" type="BoardFeatureType"
	// maxOccurs="unbounded"></xs:element>
	// <xs:element name="mountedDevice" type="BoardsDeviceType"
	// maxOccurs="unbounded"/>
	// <xs:element name="compatibleDevice" type="CompatibleDeviceType"
	// maxOccurs="unbounded"/>
	// <xs:element name="image">
	// <xs:complexType>
	// <xs:attribute name="small" type="xs:string" use="optional"/>
	// <xs:attribute name="large" type="xs:string" use="optional"/>
	// </xs:complexType>
	// </xs:element>
	// <xs:element name="debugInterface" type="DebugInterfaceType"
	// maxOccurs="unbounded"/>
	// <xs:element name="book" type="BoardsBookType" maxOccurs="unbounded"/>
	// </xs:choice>
	// </xs:group>

	private void processBoard(Element el, TreeNode parent) {

		// Required
		String boardVendor = el.getAttribute("vendor").trim();
		String boardName = el.getAttribute("name").trim();

		// Optional
		String boardRevision = el.getAttribute("revision").trim();
		String salesContact = el.getAttribute("salesContact").trim();
		String orderForm = el.getAttribute("orderForm").trim();

		TreeNode boardNode = new TreeNode(TreeNode.BOARD_TYPE);
		parent.addChild(boardNode);

		boardNode.setName(boardName);
		boardNode.putProperty(TreeNode.VENDOR_PROPERTY, boardVendor);
		boardNode
				.putNonEmptyProperty(TreeNode.REVISION_PROPERTY, boardRevision);

		String boardDescription = "Board: " + boardName;

		String descriptionTail = "";
		descriptionTail = extendDescription(descriptionTail, "revision",
				boardRevision);
		descriptionTail = extendDescription(descriptionTail, "vendor",
				boardVendor);
		descriptionTail = extendDescription(descriptionTail, "salesContact",
				salesContact);
		descriptionTail = extendDescription(descriptionTail, "orderForm",
				orderForm);

		List<Element> childElements = Utils.getChildElementsList(el);

		for (Element childElement : childElements) {

			String elementName = childElement.getNodeName();

			if (isBrief()) {
				if ("description".equals(elementName)) {

					boardDescription = extendDescription(boardDescription,
							Utils.getElementContent(childElement));

				}
			} else {
				if ("description".equals(elementName)) {

					boardDescription = extendDescription(boardDescription,
							Utils.getElementContent(childElement));

				} else if ("image".equals(elementName)) {

					// Ignored

				} else if ("book".equals(elementName)) {

					// <xs:element name="book" type="BoardsBookType"
					// maxOccurs="unbounded"/>

					processBook(childElement, boardNode);

				} else if ("mountedDevice".equals(elementName)) {

					// <xs:complexType name="BoardsDeviceType">
					// <xs:attribute name="deviceIndex" type="xs:string"
					// use="optional" />
					// <xs:attribute name="Dvendor" type="DeviceVendorEnum"
					// use="required"/>
					// <xs:attribute name="Dfamily" type="xs:string"
					// use="optional"/>
					// <xs:attribute name="DsubFamily" type="xs:string"
					// use="optional"/>
					// <xs:attribute name="Dname" type="xs:string"
					// use="optional"/>
					// </xs:complexType>

					// Required
					String Dvendor = childElement.getAttribute("Dvendor")
							.trim();

					// Optional
					String deviceIndex = childElement.getAttribute(
							"deviceIndex").trim();
					String Dfamily = childElement.getAttribute("Dfamily")
							.trim();
					String DsubFamily = childElement.getAttribute("DsubFamily")
							.trim();
					String Dname = childElement.getAttribute("Dname").trim();

					TreeNode deviceNode = new TreeNode(TreeNode.DEVICE_TYPE);
					boardNode.addChild(deviceNode);

					String name = "";
					name = extendName(name, Dfamily);
					name = extendName(name, DsubFamily);
					name = extendName(name, Dname);

					deviceNode.setName(name);

					String deviceDescription = "Mounted device: " + name;
					deviceDescription = extendDescription(deviceDescription,
							"deviceIndex", deviceIndex);
					deviceDescription = extendDescription(deviceDescription,
							"Dvendor", Dvendor);
					deviceDescription = extendDescription(deviceDescription,
							"Dfamily", Dfamily);
					deviceDescription = extendDescription(deviceDescription,
							"DsubFamily", DsubFamily);
					deviceDescription = extendDescription(deviceDescription,
							"Dname", Dname);

					String va[] = Dvendor.split(":");
					deviceNode.putProperty(TreeNode.VENDOR_PROPERTY, va[0]);
					deviceNode.putProperty(TreeNode.VENDORID_PROPERTY, va[1]);

					deviceNode.setDescription(deviceDescription);

				} else if ("compatibleDevice".equals(elementName)) {

					// <xs:complexType name="CompatibleDeviceType">
					// <xs:attribute name="deviceIndex" type="xs:string"
					// use="optional" />
					// <xs:attribute name="Dvendor" type="DeviceVendorEnum"
					// use="optional"/>
					// <xs:attribute name="Dfamily" type="xs:string"
					// use="optional"/>
					// <xs:attribute name="DsubFamily" type="xs:string"
					// use="optional"/>
					// <xs:attribute name="Dname" type="xs:string"
					// use="optional"/>
					// </xs:complexType>

					// Optional
					String deviceIndex = childElement.getAttribute(
							"deviceIndex").trim();
					String Dvendor = childElement.getAttribute("Dvendor")
							.trim();
					String Dfamily = childElement.getAttribute("Dfamily")
							.trim();
					String DsubFamily = childElement.getAttribute("DsubFamily")
							.trim();
					String Dname = childElement.getAttribute("Dname").trim();

					TreeNode deviceNode = new TreeNode(TreeNode.DEVICE_TYPE);
					boardNode.addChild(deviceNode);

					String name = "";
					name = extendName(name, Dfamily);
					name = extendName(name, DsubFamily);
					name = extendName(name, Dname);

					deviceNode.setName(name);

					String deviceDescription = "Compatible device: " + name;
					deviceDescription = extendDescription(deviceDescription,
							"deviceIndex", deviceIndex);
					deviceDescription = extendDescription(deviceDescription,
							"Dvendor", Dvendor);
					deviceDescription = extendDescription(deviceDescription,
							"Dfamily", Dfamily);
					deviceDescription = extendDescription(deviceDescription,
							"DsubFamily", DsubFamily);
					deviceDescription = extendDescription(deviceDescription,
							"Dname", Dname);

					String va[] = Dvendor.split(":");
					deviceNode.putProperty(TreeNode.VENDOR_PROPERTY, va[0]);
					deviceNode.putProperty(TreeNode.VENDORID_PROPERTY, va[1]);

					deviceNode.setDescription(deviceDescription);

				} else if ("feature".equals(elementName)) {

					// <xs:element name="feature" type="BoardFeatureType"
					// maxOccurs="unbounded"></xs:element>

					processFeature(childElement, boardNode);

				} else if ("debugInterface".equals(elementName)) {

					// <xs:complexType name="DebugInterfaceType">
					// <xs:attribute name="adapter" type="xs:string"/>
					// <xs:attribute name="connector" type="xs:string"/>
					// </xs:complexType>

					String adapter = childElement.getAttribute("adapter")
							.trim();
					String connector = childElement.getAttribute("connector")
							.trim();

					TreeNode debugNode = new TreeNode(
							TreeNode.DEBUGINTERFACE_TYPE);
					boardNode.addChild(debugNode);

					debugNode.setName(adapter);
					debugNode.putProperty(TreeNode.CONNECTOR_PROPERTY,
							connector);

					String description = "Debug interface: " + adapter;
					description = extendDescription(description, "connector",
							connector);
					debugNode.setDescription(description);

				} else {
					System.out.println("Not processed <" + elementName + ">");
				}
			}
		}

		boardDescription = extendDescription(boardDescription, descriptionTail);
		boardNode.setDescription(boardDescription);
	}

	// <xs:complexType name="ExampleType">
	// <xs:sequence>
	// <!-- brief example description -->
	// <xs:element name="description" type="xs:string"/>
	// <!-- references the board -->
	// <xs:element name="board" type="BoardReferenceType"
	// maxOccurs="unbounded"/>
	// <!-- lists environments with their load files -->
	// <xs:element name="project" type="ExampleProjectType"/>
	// <!-- categories, keywords and used components -->
	// <xs:element name="attributes" type="ExampleAttributesType" />
	// </xs:sequence>
	// <!-- display name of the example -->
	// <xs:attribute name="name" type="xs:string" use="required"/>
	// <!-- relative folder where the example is stored in the package -->
	// <xs:attribute name="folder" type="xs:string" use="required"/>
	// <!-- archive file name with extension located in folder -->
	// <xs:attribute name="archive" type="xs:string" use="optional"/>
	// <!-- file name with extension relative to folder -->
	// <xs:attribute name="doc" type="xs:string" use="required"/>
	// <!-- version of the example -->
	// <xs:attribute name="version" type="xs:string" use="optional"/>
	// </xs:complexType>

	private void processExample(Element el, TreeNode parent, boolean isFlat) {

		// Required
		String exampleName = el.getAttribute("name").trim();
		String exampleFolder = el.getAttribute("folder").trim();
		String exampleDoc = el.getAttribute("doc").trim();

		// Optional
		String exampleVendor = el.getAttribute("vendor").trim();
		String exampleVersion = el.getAttribute("version").trim();
		String exampleArchive = el.getAttribute("archive").trim();

		TreeNode exampleNode = new TreeNode(TreeNode.EXAMPLE_TYPE);
		parent.addChild(exampleNode);

		exampleNode.putProperty(TreeNode.VENDOR_PROPERTY, exampleVendor);

		TreeNode linkNode;
		if (isFlat) {
			// Linearise, add all children to the outline node
			linkNode = parent;
		} else {
			linkNode = exampleNode;
		}

		String posixDoc = updatePosixSeparators(exampleDoc);
		String posixFolder = updatePosixSeparators(exampleFolder);
		linkNode.putProperty(TreeNode.FOLDER_PROPERTY, posixFolder);

		linkNode.putNonEmptyProperty(TreeNode.VERSION_PROPERTY, exampleVersion);
		linkNode.putNonEmptyProperty(TreeNode.ARCHIVE_PROPERTY, exampleArchive);

		String exampleDescription = "Example: " + exampleName;

		String descriptionTail = "";
		descriptionTail = extendDescription(descriptionTail, "folder",
				posixFolder);
		descriptionTail = extendDescription(descriptionTail, "doc", posixDoc);
		descriptionTail = extendDescription(descriptionTail, "version",
				exampleVersion);
		descriptionTail = extendDescription(descriptionTail, "archive",
				exampleArchive);

		List<Element> childElements = Utils.getChildElementsList(el);
		for (Element childElement : childElements) {

			String elementName = childElement.getNodeName();

			if (isBrief()) {

				if ("description".equals(elementName)) {

					exampleDescription = extendDescription(exampleDescription,
							Utils.getElementContent(childElement));

				} else if ("board".equals(elementName)) {

					processBoardRef(childElement, linkNode);

				}

			} else {

				if ("description".equals(elementName)) {

					exampleDescription = extendDescription(exampleDescription,
							Utils.getElementContent(childElement));

				} else if ("board".equals(elementName)) {

					processBoardRef(childElement, linkNode);

				} else if ("project".equals(elementName)) {

					// <xs:complexType name="ExampleProjectType">
					// <xs:sequence>
					// <xs:element name="environment" maxOccurs="unbounded">
					// <xs:complexType>
					// <xs:attribute name="name" type="xs:string"
					// use="required"/>
					// <xs:attribute name="load" type="xs:string"
					// use="required"/>
					// </xs:complexType>
					// </xs:element>
					// </xs:sequence>
					// </xs:complexType>

					List<Element> childElements2 = Utils
							.getChildElementsList(childElement);
					for (Element childElement2 : childElements2) {

						String elementName2 = childElement2.getNodeName();
						if ("environment".equals(elementName2)) {

							// Required
							String name = childElement2.getAttribute("name")
									.trim();
							String load = childElement2.getAttribute("load")
									.trim();

							TreeNode environmentNode = new TreeNode(
									TreeNode.ENVIRONMENT_TYPE);
							linkNode.addChild(environmentNode);

							environmentNode.setName(load + " (" + name + ")");

							String description = "Environment";
							description = extendDescription(description,
									"name", name);
							description = extendDescription(description,
									"load", load);
							environmentNode.setDescription(description);

						} else {
							System.out.println("Not processed <" + elementName2
									+ ">");
						}
					}

				} else if ("attributes".equals(elementName)) {

					// <xs:complexType name="ExampleAttributesType">
					// <xs:choice maxOccurs="unbounded">
					// <xs:element name="category" type="xs:string"
					// minOccurs="0"
					// maxOccurs="unbounded"/>
					// <xs:element name="component" type="ComponentCategoryType"
					// minOccurs="0" maxOccurs="unbounded"/>
					// <xs:element name="keyword" type="xs:string" minOccurs="0"
					// maxOccurs="unbounded"/>
					// </xs:choice>
					// </xs:complexType>

					List<Element> childElements2 = Utils
							.getChildElementsList(childElement);
					for (Element childElement2 : childElements2) {

						String elementName2 = childElement2.getNodeName();
						if ("category".equals(elementName2)) {

							TreeNode categoryNode = new TreeNode(
									TreeNode.CATEGORY_TYPE);
							linkNode.addChild(categoryNode);

							String category = Utils
									.getElementContent(childElement2);
							categoryNode.setName(category);
							categoryNode
									.setDescription("Category: " + category);

						} else if ("component".equals(elementName2)) {

							// <xs:complexType name="ComponentCategoryType">
							// <xs:attribute name="Cclass" type="CclassType"
							// use="required"/>
							// <xs:attribute name="Cgroup" type="CgroupType"
							// use="optional"/>
							// <xs:attribute name="Csub" type="CsubType"
							// use="optional"/>
							// <xs:attribute name="Cversion" type="VersionType"
							// use="optional"/>
							// <xs:attribute name="Cvendor" type="xs:string"
							// use="optional"/>
							// </xs:complexType>

							// Required
							String Cclass = childElement2
									.getAttribute("Cclass").trim();

							// Optional
							String Cgroup = childElement2
									.getAttribute("Cgroup").trim();
							String Csub = childElement2.getAttribute("Csub")
									.trim();
							String Cversion = childElement2.getAttribute(
									"Cversion").trim();
							String Cvendor = childElement2.getAttribute(
									"Cvendor").trim();

							TreeNode componentNode = new TreeNode(
									TreeNode.COMPONENT_TYPE);
							linkNode.addChild(componentNode);

							String name = "";
							name = extendName(name, Cclass);
							name = extendName(name, Cgroup);
							name = extendName(name, Csub);

							componentNode.setName(name);

							String description = "Referred component: " + name;
							description = extendDescription(description,
									"Cvendor", Cvendor);
							description = extendDescription(description,
									"Cclass", Cclass);
							description = extendDescription(description,
									"Cgroup", Cgroup);
							description = extendDescription(description,
									"Csub", Csub);
							description = extendDescription(description,
									"Cversion", Cversion);

							componentNode.setDescription(description);

						} else if ("keyword".equals(elementName2)) {

							processKeyword(childElement2, linkNode);

						} else {
							System.out.println("Not processed <" + elementName2
									+ ">");
						}
					}
				} else {
					System.out.println("Not processed <" + elementName + ">");
				}
			}
		}

		String boardName = "";
		for (TreeNode childNode : linkNode.getChildrenArray()) {

			if (TreeNode.BOARD_TYPE.equals(childNode.getType())) {
				boardName = childNode.getName();
				break;
			}
		}

		if (boardName.length() > 0) {
			exampleName = exampleName + " (" + boardName + ")";
		}
		exampleNode.setName(exampleName);

		if (isBrief()) {
			exampleNode.removeChildren();
		}

		exampleDescription = extendDescription(exampleDescription,
				descriptionTail);
		exampleNode.setDescription(exampleDescription);
	}

	private void processBoardRef(Element el, TreeNode parent) {

		// <xs:complexType name="BoardReferenceType">
		// <xs:attribute name="name" type="xs:string" use="required"/>
		// <!-- refers to Board Description by name -->
		// <xs:attribute name="vendor" type="xs:string" use="required"/>
		// <xs:attribute name="Dvendor" type="DeviceVendorEnum"
		// use="optional"/> <!-- deprecated in 1.1 -->
		// <xs:attribute name="Dfamily" type="xs:string"
		// use="optional"/> <!-- deprecated in 1.1 -->
		// <xs:attribute name="DsubFamily" type="xs:string"
		// use="optional"/> <!-- deprecated in 1.1 -->
		// <xs:attribute name="Dname" type="xs:string" use="optional"/>
		// <!-- deprecated in 1.1 -->
		// </xs:complexType>

		// Required
		String boardName = el.getAttribute("name").trim();
		String boardVendor = el.getAttribute("vendor").trim();

		TreeNode boardNode = new TreeNode(TreeNode.BOARD_TYPE);
		parent.addChild(boardNode);

		boardNode.setName(boardName);
		boardNode.putProperty(TreeNode.VENDOR_PROPERTY, boardVendor);

		String description = "Board: " + boardName;
		description = extendDescription(description, "vendor", boardVendor);
		boardNode.setDescription(description);
	}

	// <xs:element name="component" minOccurs="1" maxOccurs="unbounded">
	// <xs:complexType>
	// <xs:sequence>
	// <!-- a component can be deprecated if it is no longer maintained-->
	// <xs:element name="deprecated" type="xs:boolean" minOccurs="0"
	// default="false"/>
	// <!-- short component description displayed -->
	// <xs:element name="description" type="xs:string"/>
	// <!-- content to be added to generated RTE_Component.h file -->
	// <xs:element name="RTE_Components_h" type="xs:string" minOccurs="0"/>
	// <!-- list of files / content -->
	// <xs:element name="files">
	// <xs:complexType>
	// <xs:sequence>
	// <xs:element name="file" type="FileType" maxOccurs="unbounded"/>
	// </xs:sequence>
	// </xs:complexType>
	// </xs:element>
	// </xs:sequence>
	// <!-- component identity attributes -->
	// <xs:attribute name="Cvendor" type="xs:string" use="optional"/>
	// <xs:attribute name="Cclass" type="CclassType" use="required"/>
	// <xs:attribute name="Cgroup" type="CgroupType" use="required"/>
	// <xs:attribute name="Csub" type="CsubType" use="optional"/>
	// <xs:attribute name="Cvariant" type="CvariantType" use="optional"/>
	// <xs:attribute name="Cversion" type="VersionType" use="required"/>
	// <!-- api version for this component -->
	// <xs:attribute name="Capiversion" type="VersionType" use="optional"/>
	// <!-- component attribute for referencing a condition specified in
	// conditions section above -->
	// <xs:attribute name="condition" type="xs:string" use="optional"/>
	// <!-- maximum allowed number of instances of a component in a project,
	// default - 1-->
	// <xs:attribute name="maxInstances" type="MaxInstancesType"
	// use="optional"/>
	// </xs:complexType>
	// </xs:element>

	// Bundle element:
	// <xs:element name="component" minOccurs="1" maxOccurs="unbounded">
	// <xs:complexType>
	// <xs:sequence>
	// <!-- a component can be deprecated if it is no longer maintained-->
	// <xs:element name="deprecated" type="xs:boolean" minOccurs="0"
	// default="false"/>
	// <!-- short component description displayed -->
	// <xs:element name="description" type="xs:string"/>
	// <!-- content to be added to generated RTE_Component.h file -->
	// <xs:element name="RTE_Components_h" type="xs:string" minOccurs="0"/>
	// <!-- list of files / content -->
	// <xs:element name="files">
	// <xs:complexType>
	// <xs:sequence>
	// <xs:element name="file" type="FileType" maxOccurs="unbounded"/>
	// </xs:sequence>
	// </xs:complexType>
	// </xs:element>
	// </xs:sequence>
	// <!-- component identity attributes Cclass Cvendor and Cversion are
	// specified by bundle -->
	// <xs:attribute name="Cgroup" type="CgroupType" use="required"/>
	// <xs:attribute name="Csub" type="CsubType" use="optional"/>
	// <xs:attribute name="Cvariant" type="CvariantType" use="optional"/>
	// <!-- api version for this component -->
	// <xs:attribute name="Capiversion" type="VersionType" use="optional"/>
	// <!-- component attribute for referencing a condition specified in
	// conditions section above -->
	// <xs:attribute name="condition" type="xs:string" use="optional"/>
	// <!-- maximum allowed number of instances of a component in a project,
	// default - 1-->
	// <xs:attribute name="maxInstances" type="MaxInstancesType"
	// use="optional"/>
	// </xs:complexType>
	// </xs:element>

	private void processComponent(Element el, TreeNode parent,
			String parentClass) {

		// Required
		String Cclass = el.getAttribute("Cclass").trim(); // may be inherited
		String Cgroup = el.getAttribute("Cgroup").trim();
		String Cversion = el.getAttribute("Cversion").trim();

		// Optional
		String Csub = el.getAttribute("Csub").trim();
		String Cvariant = el.getAttribute("Cvariant").trim();
		String Cvendor = el.getAttribute("Cvendor").trim();

		String apiVersion = el.getAttribute("Capiversion").trim();
		String condition = el.getAttribute("condition").trim();
		String maxInstances = el.getAttribute("maxInstances").trim();

		TreeNode componentNode = new TreeNode(TreeNode.COMPONENT_TYPE);
		parent.addChild(componentNode);

		String name = "";
		name = extendName(name, Cvendor);
		name = extendName(name, Cclass.length() > 0 ? Cclass : parentClass);
		name = extendName(name, Cgroup);
		name = extendName(name, Csub);
		name = extendName(name, Cvariant);

		componentNode.setName(name);

		String componentDescription = "Component: " + name;

		String descriptionTail = "";
		descriptionTail = extendDescription(descriptionTail, "Cvendor", Cvendor);
		descriptionTail = extendDescription(descriptionTail, "Cclass", Cclass);
		descriptionTail = extendDescription(descriptionTail, "Cgroup", Cgroup);
		descriptionTail = extendDescription(descriptionTail, "Csub", Csub);
		descriptionTail = extendDescription(descriptionTail, "Cvariant",
				Cvariant);
		descriptionTail = extendDescription(descriptionTail, "Cversion",
				Cversion);
		descriptionTail = extendDescription(descriptionTail, "condition",
				condition);
		descriptionTail = extendDescription(descriptionTail, "apiVersion",
				apiVersion);
		descriptionTail = extendDescription(descriptionTail, "maxInstances",
				maxInstances);

		componentNode.putNonEmptyProperty(TreeNode.CONDITION_PROPERTY,
				condition);
		componentNode.putNonEmptyProperty(TreeNode.APIVERSION_PROPERTY,
				apiVersion);
		componentNode.putNonEmptyProperty(TreeNode.MAXINSTANCES_PROPERTY,
				maxInstances);

		List<Element> childElements = Utils.getChildElementsList(el);

		for (Element childElement : childElements) {

			String elementName = childElement.getNodeName();
			if ("description".equals(elementName)) {

				componentDescription = extendDescription(componentDescription,
						Utils.getElementContent(childElement));

			} else if ("RTE_Components_h".equals(elementName)) {

				// <!-- content to be added to generated RTE_Component.h file
				// -->
				// <xs:element name="RTE_Components_h" type="xs:string"
				// minOccurs="0"/>

				String rte = Utils.getElementContent(childElement);
				componentNode.putNonEmptyProperty(TreeNode.RTE_PROPERTY, rte);

			} else if ("deprecated".equals(elementName)) {

				// <!-- a component can be deprecated if it is no longer
				// maintained-->
				// <xs:element name="deprecated" type="xs:boolean" minOccurs="0"
				// default="false"/>

				String deprecated = Utils.getElementContent(childElement);
				componentNode.putNonEmptyProperty(TreeNode.DEPRECATED_PROPERTY,
						deprecated);

			} else if ("files".equals(elementName)) {

				// <xs:element name="files">
				// <xs:complexType>
				// <xs:sequence>
				// <xs:element name="file" type="FileType"
				// maxOccurs="unbounded"/>
				// </xs:sequence>
				// </xs:complexType>
				// </xs:element>

				List<Element> childElements2 = Utils
						.getChildElementsList(childElement);
				for (Element childElement2 : childElements2) {
					String elementName2 = childElement2.getNodeName();
					if ("file".equals(elementName2)) {

						processFile(childElement2, componentNode);

					} else {
						System.out.println("Not processed <" + elementName2
								+ ">");
					}
				}
			} else {
				System.out.println("Not processed <" + elementName + ">");
			}
		}

		componentDescription = extendDescription(componentDescription,
				descriptionTail);
		componentNode.setDescription(componentDescription);

	}

	// <!-- Component file type definition -->
	// <xs:complexType name="FileType">
	// <xs:attribute name="condition" type="xs:string" use="optional"/>
	// <!-- file item category: source, header, include path, etc. -->
	// <xs:attribute name="category" type="FileCategoryType" use ="required"/>
	// <!-- file item action attribute : config (copy to project, template,
	// interface) -->
	// <xs:attribute name="attr" type="FileAttributeType" use ="optional"/>
	// <!-- description for "template" or "interface" files. Multiple items are
	// combined when they have the same select attribute -->
	// <xs:attribute name="select" type="xs:string" use ="optional"/>
	// <!-- path + filename + extension -->
	// <xs:attribute name="name" type ="xs:string" use="required" />
	// <!-- copy file to project folder: deprecated, use attr="config" instead
	// -->
	// <xs:attribute name="copy" type ="xs:string" use="optional"/>
	// <!-- simple file version: to be used by RTE copy file action to see
	// whether the file needs updating in project -->
	// <xs:attribute name="version" type ="VersionType" use="optional"/>
	// <!-- path(s) to find source files for a library, paths are delimited with
	// semicolon (;) -->
	// <xs:attribute name="src" type="xs:string" use ="optional"/>
	// </xs:complexType>

	private void processFile(Element el, TreeNode parent) {

		// Required
		String name = el.getAttribute("name").trim();
		String category = el.getAttribute("category").trim();

		// Optional
		String attr = el.getAttribute("attr").trim();
		String version = el.getAttribute("version").trim();
		String src = el.getAttribute("src").trim();
		String select = el.getAttribute("select").trim();
		String condition = el.getAttribute("condition").trim();

		TreeNode fileNode = new TreeNode(TreeNode.FILE_TYPE);
		parent.addChild(fileNode);

		String posixPath = updatePosixSeparators(name);
		String pathArray[] = posixPath.split("/");
		String baseName = pathArray[pathArray.length - 1];

		fileNode.putProperty(TreeNode.FILE_PROPERTY, posixPath);
		fileNode.putProperty(TreeNode.CATEGORY_PROPERTY, category);
		fileNode.putNonEmptyProperty(TreeNode.ATTR_PROPERTY, attr);
		fileNode.putNonEmptyProperty(TreeNode.VERSION_PROPERTY, version);
		fileNode.putNonEmptyProperty(TreeNode.SRC_PROPERTY, src);
		fileNode.putNonEmptyProperty(TreeNode.SELECT_PROPERTY, select);
		fileNode.putNonEmptyProperty(TreeNode.CONDITION_PROPERTY, condition);

		String description;
		if ("include".equals(category)) {
			fileNode.setName(posixPath);
			description = "Include: ";
		} else {
			fileNode.setName(baseName);
			description = "File: ";
		}

		description += posixPath;
		description = extendDescription(description, "category", category);
		description = extendDescription(description, "attr", attr);
		description = extendDescription(description, "version", version);
		description = extendDescription(description, "src", src);
		description = extendDescription(description, "select", select);
		description = extendDescription(description, "condition", condition);

		fileNode.setDescription(description);
	}

	// <xs:element name="bundle" minOccurs="1" maxOccurs="unbounded">
	// <xs:complexType>
	// <xs:sequence>
	// <xs:element name="description" type="xs:string"/>
	// <xs:element name="doc" type="xs:string"/>
	// <xs:element name="component" minOccurs="1" maxOccurs="unbounded">
	// <xs:complexType>
	// <xs:sequence>
	// <!-- a component can be deprecated if it is no longer maintained-->
	// <xs:element name="deprecated" type="xs:boolean" minOccurs="0"
	// default="false"/>
	// <!-- short component description displayed -->
	// <xs:element name="description" type="xs:string"/>
	// <!-- content to be added to generated RTE_Component.h file -->
	// <xs:element name="RTE_Components_h" type="xs:string" minOccurs="0"/>
	// <!-- list of files / content -->
	// <xs:element name="files">
	// <xs:complexType>
	// <xs:sequence>
	// <xs:element name="file" type="FileType" maxOccurs="unbounded"/>
	// </xs:sequence>
	// </xs:complexType>
	// </xs:element>
	// </xs:sequence>
	// <!-- component identity attributes Cclass Cvendor and Cversion are
	// specified by bundle -->
	// <xs:attribute name="Cgroup" type="CgroupType" use="required"/>
	// <xs:attribute name="Csub" type="CsubType" use="optional"/>
	// <xs:attribute name="Cvariant" type="CvariantType" use="optional"/>
	// <!-- api version for this component -->
	// <xs:attribute name="Capiversion" type="VersionType" use="optional"/>
	// <!-- component attribute for referencing a condition specified in
	// conditions section above -->
	// <xs:attribute name="condition" type="xs:string" use="optional"/>
	// <!-- maximum allowed number of instances of a component in a project,
	// default - 1-->
	// <xs:attribute name="maxInstances" type="MaxInstancesType"
	// use="optional"/>
	// </xs:complexType>
	// </xs:element>
	// </xs:sequence>
	// <!-- bundle attributes -->
	// <xs:attribute name="Cbundle" type="xs:string" use="required"/>
	// <xs:attribute name="Cvendor" type="xs:string" use="optional"/>
	// <xs:attribute name="Cclass" type="CclassType" use="required"/>
	// <xs:attribute name="Cversion" type="VersionType" use="required"/>
	// </xs:complexType>
	// </xs:element>

	private void processBundle(Element el, TreeNode parent) {

		// Required
		String Cbundle = el.getAttribute("Cbundle").trim();
		String Cclass = el.getAttribute("Cclass").trim();
		String Cversion = el.getAttribute("Cversion").trim();

		// Optional
		String Cvendor = el.getAttribute("Cvendor").trim();

		TreeNode bundleNode = new TreeNode(TreeNode.BUNDLE_TYPE);
		parent.addChild(bundleNode);

		String name = "";
		name = extendName(name, Cclass);
		name = extendName(name, Cbundle);
		bundleNode.setName(name);

		String bundleDescription = "Bundle: " + name;

		String descriptionTail = "";
		descriptionTail = extendDescription(descriptionTail, "Cvendor", Cvendor);
		descriptionTail = extendDescription(descriptionTail, "Cbundle", Cbundle);
		descriptionTail = extendDescription(descriptionTail, "Cclass", Cclass);
		descriptionTail = extendDescription(descriptionTail, "Cversion",
				Cversion);

		List<Element> childElements = Utils.getChildElementsList(el);

		for (Element childElement : childElements) {

			String elementName = childElement.getNodeName();
			if ("description".equals(elementName)) {

				bundleDescription = extendDescription(bundleDescription,
						Utils.getElementContent(childElement));

			} else if ("doc".equals(elementName)) {

				String doc = Utils.getElementContent(childElement);
				String posixDoc = updatePosixSeparators(doc);
				descriptionTail = extendDescription(descriptionTail, "doc",
						posixDoc);
				bundleNode.putNonEmptyProperty(TreeNode.URL_PROPERTY, doc);

			} else if ("component".equals(elementName)) {

				processComponent(childElement, bundleNode, Cclass);

			} else {
				System.out.println("Not processed <" + elementName + ">");
			}
		}

		bundleDescription = extendDescription(bundleDescription,
				descriptionTail);
		bundleNode.setDescription(bundleDescription);
	}

	// <!-- taxonomy description type definition -->
	// <xs:complexType name="TaxonomyDescriptionType">
	// <xs:simpleContent>
	// <xs:extension base='xs:string'>
	// <xs:attribute name="Cclass" type="CclassType" use="required"/>
	// <xs:attribute name="Cgroup" type="CgroupType" use="optional"/>
	// <xs:attribute name="doc" type="xs:string" use="optional"/>
	// <xs:attribute name="generator" type="xs:string" use="optional"/>
	// </xs:extension>
	// </xs:simpleContent>
	// </xs:complexType>

	private void processTaxonomyDescription(Element el, TreeNode parent) {

		// Required
		String Cclass = el.getAttribute("Cclass").trim();
		// Optional

		String Cgroup = el.getAttribute("Cgroup").trim();
		String doc = el.getAttribute("doc").trim();
		String generator = el.getAttribute("generator").trim();

		TreeNode taxonomyNode = new TreeNode(TreeNode.TAXONOMY_TYPE);

		String name = Cclass;
		name = extendName(name, Cgroup);

		taxonomyNode.setName(name);

		String posixDoc = updatePosixSeparators(doc);
		String description = "Taxonomy: " + name;
		description += Utils.getElementContent(el);
		description = extendDescription(description, "Cclass", Cclass);
		description = extendDescription(description, "Cgroup", Cgroup);
		description = extendDescription(description, "doc", posixDoc);
		description = extendDescription(description, "generator", generator);

		taxonomyNode.setDescription(description);

		parent.addChild(taxonomyNode);
	}

	// <xs:element name="keyword" type="xs:string" maxOccurs="unbounded"/>

	private void processKeyword(Element el, TreeNode parent) {

		TreeNode keywordNode = new TreeNode(TreeNode.KEYWORD_TYPE);
		String keyword = Utils.getElementContent(el);
		keywordNode.setName(keyword);
		keywordNode.setDescription("Keyword: " + keyword);

		parent.addChild(keywordNode);
	}

	// <xs:complexType name="ConditionType">
	// <xs:sequence>
	// <xs:element name="description" type="xs:string" minOccurs="0"/>
	// <xs:choice minOccurs="1" maxOccurs="unbounded">
	// <xs:element name="accept" type="FilterType"/>
	// <xs:element name="require" type="FilterType"/>
	// <xs:element name="deny" type="FilterType"/>
	// </xs:choice>
	// </xs:sequence>
	// <xs:attribute name="id" type="xs:string" use="required"/>
	// </xs:complexType>

	private void processCondition(Element el, TreeNode parent) {

		// Required
		String id = el.getAttribute("id").trim();

		TreeNode conditionNode = new TreeNode(TreeNode.CONDITION_TYPE);
		parent.addChild(conditionNode);

		conditionNode.setName(id);

		String conditionDescription = "Condition: " + id;

		List<Element> childElements = Utils.getChildElementsList(el);

		for (Element childElement : childElements) {

			String elementName = childElement.getNodeName();
			if ("description".equals(elementName)) {

				conditionDescription = extendDescription(conditionDescription,
						Utils.getElementContent(childElement));

			} else if ("accept".equals(elementName)
					|| "require".equals(elementName)
					|| "deny".equals(elementName)) {

				// <xs:complexType name="FilterType">
				// <xs:attribute name="Dfamily" type="xs:string"/>
				// <xs:attribute name="DsubFamily" type="xs:string"/>
				// <xs:attribute name="Dvariant" type="xs:string"/>
				// <xs:attribute name="Dvendor" type="DeviceVendorEnum"/>
				// <xs:attribute name="Dname" type="xs:string"/>
				// <xs:attribute name="Dcore" type="DcoreEnum"/>
				// <xs:attribute name="Dfpu" type="DfpuEnum"/>
				// <xs:attribute name="Dmpu" type="DmpuEnum"/>
				// <xs:attribute name="Dendian" type="DendianEnum"/>
				// <xs:attribute name="Cvendor" type="xs:string"/>
				// <xs:attribute name="Cbundle" type="xs:string"/>
				// <xs:attribute name="Cclass" type="CclassType"/>
				// <xs:attribute name="Cgroup" type="CgroupType"/>
				// <xs:attribute name="Csub" type="CsubType"/>
				// <xs:attribute name="Cvariant" type="CvariantType"/>
				// <xs:attribute name="Cversion" type="VersionType"/>
				// <xs:attribute name="Capiversion" type="VersionType"/>
				// <xs:attribute name="Tcompiler" type="CompilerEnumType"/>
				// <xs:attribute name="condition" type="xs:string"/>
				// </xs:complexType>

				String conditionName = "";
				String description = "";

				TreeNode node = null;
				if ("accept".equals(elementName)) {
					node = new TreeNode(TreeNode.ACCEPT_TYPE);
					description = "Accept";
				} else if ("require".equals(elementName)) {
					node = new TreeNode(TreeNode.REQUIRE_TYPE);
					description = "Require";
				} else if ("deny".equals(elementName)) {
					node = new TreeNode(TreeNode.DENY_TYPE);
					description = "Deny";
				}
				conditionNode.addChild(node);

				String sa[] = { "Dfamily", "DsubFamily", "Dvariant", "Dvendor",
						"Dname", "Dcore", "Dfpu", "Dmpu", "Dendian", "Cvendor",
						"Cbundle", "Cclass", "Cgroup", "Csub", "Cvariant",
						"Cversion", "Capiversion", "Tcompiler", "condition" };
				List<String> attrNames = Utils.getAttributesNames(childElement,
						sa);
				for (String attrName : attrNames) {

					String value = childElement.getAttribute(attrName).trim();

					if ("Dvendor".equals(attrName)) {
						conditionName = extendName(conditionName,
								value.split(":")[0]);
					} else {
						conditionName = extendName(conditionName, value);
					}

					description = extendDescription(description, attrName,
							value);
				}

				node.setName(conditionName);
				node.setDescription(description);

			} else {
				System.out.println("Not processed <" + elementName + ">");
			}
		}

		conditionNode.setDescription(conditionDescription);
	}

	// <!-- API type definition -->
	// <xs:complexType name="ApiType">
	// <xs:sequence>
	// <xs:element name="description" type="xs:string" minOccurs="0"/>
	// <!-- list of files / content -->
	// <xs:element name="files">
	// <xs:complexType>
	// <xs:sequence>
	// <xs:element name="file" type="FileType" maxOccurs="unbounded"/>
	// </xs:sequence>
	// </xs:complexType>
	// </xs:element>
	// </xs:sequence>
	// <xs:attribute name="Cclass" type="CclassType" use="required"/>
	// <xs:attribute name="Cgroup" type="CgroupType" use="required"/>
	// <xs:attribute name="exclusive" type="xs:boolean" use="optional"
	// default="1"/>
	// <xs:attribute name="Capiversion" type="VersionType" use="optional"/>
	// </xs:complexType>

	private void processApi(Element el, TreeNode parent) {

		// Required
		String Cclass = el.getAttribute("Cclass").trim();
		String Cgroup = el.getAttribute("Cgroup").trim();

		// Optional
		String exclusive = el.getAttribute("exclusive").trim();
		String apiVersion = el.getAttribute("Capiversion").trim();

		TreeNode apiNode = new TreeNode(TreeNode.API_TYPE);
		parent.addChild(apiNode);

		String name = "";
		name = extendName(name, Cclass);
		name = extendName(name, Cgroup);
		apiNode.setName(name);

		String apiDescription = "Api: " + name;

		String descriptionTail = "";
		descriptionTail = extendDescription(descriptionTail, "Cclass", Cclass);
		descriptionTail = extendDescription(descriptionTail, "Cgroup", Cgroup);
		descriptionTail = extendDescription(descriptionTail, "exclusive",
				exclusive);
		descriptionTail = extendDescription(descriptionTail, "CapiVersion",
				apiVersion);

		apiNode.putNonEmptyProperty(TreeNode.EXCLUSIVE_PROPERTY, exclusive);
		apiNode.putNonEmptyProperty(TreeNode.APIVERSION_PROPERTY, apiVersion);

		List<Element> childElements = Utils.getChildElementsList(el);

		for (Element childElement : childElements) {

			String elementName = childElement.getNodeName();
			if ("description".equals(elementName)) {

				apiDescription = extendDescription(apiDescription,
						Utils.getElementContent(childElement));

			} else if ("files".equals(elementName)) {

				List<Element> childElements2 = Utils
						.getChildElementsList(childElement);
				for (Element childElement2 : childElements2) {
					String elementName2 = childElement2.getNodeName();
					if ("file".equals(elementName2)) {

						processFile(childElement2, apiNode);

					} else {
						System.out.println("Not processed <" + elementName2
								+ ">");
					}
				}
			} else {
				System.out.println("Not processed <" + elementName + ">");
			}
		}

		apiDescription = extendDescription(apiDescription, descriptionTail);
		apiNode.setDescription(apiDescription);
	}

	// ------------------------------------------------------------------------

	public void parsePdscBrief(String url, String name, String version,
			TreeNode tree) throws IOException, ParserConfigurationException,
			SAXException {

		String fullUrl;
		if (url.endsWith("/")) {
			fullUrl = url + name;
		} else {
			fullUrl = url + "/" + name;
		}

		m_out.println("Parsing \"" + fullUrl + "\" v" + version + "...");

		URL u = new URL(fullUrl);
		InputSource inputSource = new InputSource(new InputStreamReader(
				u.openStream()));

		DocumentBuilder parser = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder();
		Document document = parser.parse(inputSource);

		Element packageElement = document.getDocumentElement();
		String firstElementName = packageElement.getNodeName();
		if (!"package".equals(firstElementName)) {
			System.out.println("Missing <packages>, <" + firstElementName
					+ "> encountered");
			return;
		}

		String schemaVersion = packageElement.getAttribute("schemaVersion")
				.trim();

		m_out.println("Schema version \"" + schemaVersion + "\".");

		if ("1.0".equals(schemaVersion)) {
			;
		} else if ("1.1".equals(schemaVersion)) {
			;
		} else if ("1.2".equals(schemaVersion)) {
			;
		} else {
			System.out.println("Unrecognised schema version " + schemaVersion);
			return;
		}

		String packName = "";
		String packDescription = "";

		Set<String> conditionKeywords = new HashSet<String>();

		// Packages
		TreeNode packNode;
		{
			TreeNode packagesNode = tree.addUniqueChild("packages", null);

			Element vendorElement = Utils.getChildElement(packageElement,
					"vendor");
			if (vendorElement == null) {
				m_out.println("Missing <vendor>.");
				return;
			}
			String packVendor = Utils.getElementContent(vendorElement);

			Element nameElement = Utils.getChildElement(packageElement, "name");
			if (nameElement == null) {
				m_out.println("Missing <name>.");
				return;
			}
			packName = Utils.getElementContent(nameElement);
			packDescription = "";

			Element descriptionElement = Utils.getChildElement(packageElement,
					"description");
			if (descriptionElement == null) {
				m_out.println("Missing <description>.");
				return;
			}
			packDescription = extendDescription(packDescription,
					Utils.getElementContent(descriptionElement));

			TreeNode vendorNode = packagesNode.addUniqueChild(
					TreeNode.VENDOR_TYPE, packVendor);

			packNode = vendorNode.getChild("package", packName);
			if (packNode != null) {
				m_out.println("Duplicate package name \"" + packName
						+ "\", ignored.");
				return;
			}

			packNode = new TreeNode("package");
			packNode.setName(packName);
			packNode.setDescription(packDescription);

			String shortUrl = url;
			if (shortUrl.endsWith("/")) {
				shortUrl = shortUrl.substring(0, shortUrl.length() - 1);
			}
			packNode.putNonEmptyProperty(TreeNode.URL_PROPERTY, shortUrl);

			packNode.putNonEmptyProperty(TreeNode.VENDOR_PROPERTY, packVendor);

			// Attach package right below vendor node
			vendorNode.addChild(packNode);

			// m_out.println("Package node \"" + packName + "\" added.");
		}

		// Kludge: use URL to detect empty package
		// TODO: use a better condition
		Element urlElement = Utils.getChildElement(packageElement, "url");
		String urlRef = Utils.getElementContent(urlElement);
		if (urlRef.length() == 0) {

			TreeNode.Condition condition = packNode.new Condition(
					TreeNode.Condition.DEPRECATED_TYPE);
			packNode.addCondition(condition);
		} else {
			packDescription = extendDescription(packDescription, "url", urlRef);
		}

		// Releases
		String currentReleaseName = null;
		String currentReleaseDescription = null;
		String currentReleaseDate = null;
		Element releasesElement = Utils.getChildElement(packageElement,
				"releases");
		if (releasesElement != null) {

			List<Element> releaseElements = Utils.getChildElementsList(
					releasesElement, "release");
			for (Element releaseElement : releaseElements) {
				String releaseName = releaseElement.getAttribute("version")
						.trim();
				String releaseDate = releaseElement.getAttribute("date").trim();
				String description = Utils.getElementContent(releaseElement);

				// Remember the top most release description
				if (currentReleaseName == null) {

					currentReleaseName = releaseName;
					currentReleaseDescription = description;
					currentReleaseDate = releaseDate;
				}

				TreeNode versionNode = packNode.addUniqueChild("version",
						releaseName);
				versionNode.setDescription(description);
				// m_out.println("Version node \"" + releaseVersion +
				// "\" added.");
			}
		} else {
			packNode.addUniqueChild("version", version);
			// m_out.println("Version node \"" + version
			// + "\" added (value from index).");
		}

		TreeNode outlineNode = new TreeNode(TreeNode.OUTLINE_TYPE);
		packNode.setOutline(outlineNode);

		TreeNode outlinePackNode = outlineNode.addUniqueChild(
				TreeNode.PACKAGE_TYPE, packName + " (brief)");
		String outlinePackDescription = "Package: " + packName;
		outlinePackDescription = extendDescription(outlinePackDescription,
				packDescription);
		outlinePackNode.setDescription(outlinePackDescription);

		if (currentReleaseName != null) {

			TreeNode outlineVersionNode = outlineNode.addUniqueChild(
					TreeNode.VERSION_TYPE, currentReleaseName);

			String versionDescription = "Version: " + currentReleaseName;
			if (currentReleaseDate.length() > 0) {
				versionDescription += ", from " + currentReleaseDate;
			}
			versionDescription = extendDescription(versionDescription,
					currentReleaseDescription);
			outlineVersionNode.setDescription(versionDescription);

			if (currentReleaseDate != null) {
				outlineVersionNode.putNonEmptyProperty(TreeNode.DATE_PROPERTY,
						currentReleaseDate);
			}
		}

		// Keywords
		Element keywordsElement = Utils.getChildElement(packageElement,
				"keywords");
		if (keywordsElement != null) {

			TreeNode keywordsNode = tree.addUniqueChild(
					TreeNode.KEYWORDS_SELECT_TYPE, null);

			List<Element> childElements2 = Utils
					.getChildElementsList(keywordsElement);
			for (Element childElement2 : childElements2) {

				String elementName2 = childElement2.getNodeName();
				if ("keyword".equals(elementName2)) {

					// Add a unique node to selection
					String keyword = Utils.getElementContent(childElement2);
					keywordsNode.addUniqueChild(TreeNode.KEYWORD_TYPE, keyword);

					// Remember condition keyword
					conditionKeywords.add(keyword);

					// Add to outline
					processKeyword(childElement2, outlineNode);
				}
			}
		}

		// Devices
		Element devicesElement = Utils.getChildElement(packageElement,
				"devices");
		if (devicesElement != null) {

			TreeNode devicesNode = tree.addUniqueChild(
					TreeNode.DEVICES_SELECT_TYPE, null);

			List<Element> familyElements = Utils.getChildElementsList(
					devicesElement, TreeNode.FAMILY_TYPE);
			for (Element familyElement : familyElements) {

				String family = familyElement.getAttribute("Dfamily").trim();
				String vendor = familyElement.getAttribute("Dvendor").trim();

				Element descriptionElement = Utils.getChildElement(
						familyElement, "description");
				String description = "Family: " + family;

				description = extendDescription(description,
						Utils.getElementContent(descriptionElement));

				String va[] = vendor.split("[:]");
				TreeNode vendorNode = devicesNode.addUniqueChild(
						TreeNode.VENDOR_TYPE, va[0]);
				if (va.length < 2) {
					// Vendor not enumeration
					continue;
				}
				vendorNode.putNonEmptyProperty("vendorid", va[1]);

				TreeNode familyNode = vendorNode.addUniqueChild("family",
						family);
				familyNode.setDescription(description);

				// Add package conditions
				TreeNode.Condition condition = packNode.new Condition(
						TreeNode.Condition.DEVICEFAMILY_TYPE);
				condition.setVendor(va[1]);
				condition.setValue(family);
				packNode.addCondition(condition);

				// Add outline node
				processFamily(familyElement, outlineNode);
			}
		}

		// Boards
		Element boardsElement = Utils.getChildElement(packageElement, "boards");
		if (boardsElement != null) {

			TreeNode boardsNode = tree.addUniqueChild(
					TreeNode.BOARDS_SELECT_TYPE, null);

			List<Element> boardElements = Utils.getChildElementsList(
					boardsElement, "board");
			for (Element boardElement : boardElements) {
				String vendor = boardElement.getAttribute("vendor").trim();
				String boardName = boardElement.getAttribute("name").trim();
				String revision = boardElement.getAttribute("revision").trim();

				Element descriptionElement = Utils.getChildElement(
						boardElement, "description");
				String description = "";
				description = Utils.getElementContent(descriptionElement);

				TreeNode vendorNode = boardsNode.addUniqueChild(
						TreeNode.VENDOR_TYPE, vendor);

				TreeNode boardNode = vendorNode.addUniqueChild(
						TreeNode.BOARD_TYPE, boardName);

				if (revision.length() > 0) {
					description += " (" + revision + ")";
				}
				boardNode.setDescription(description);

				// Add package conditions
				TreeNode.Condition condition = packNode.new Condition(
						TreeNode.Condition.BOARD_TYPE);
				condition.setVendor(vendor);
				condition.setValue(boardName);
				packNode.addCondition(condition);

				// Add outline node
				processBoard(boardElement, outlineNode);
			}
		}

		// Examples
		Element examplesElement = Utils.getChildElement(packageElement,
				"examples");
		if (examplesElement != null) {

			List<Element> exampleElements = Utils.getChildElementsList(
					examplesElement, "example");
			for (Element exampleElement : exampleElements) {

				Element attributesElement = Utils.getChildElement(
						exampleElement, "attributes");
				if (attributesElement != null) {

					List<Element> keywordElements = Utils.getChildElementsList(
							attributesElement, "keyword");

					TreeNode keywordsNode = tree.addUniqueChild(
							TreeNode.KEYWORDS_SELECT_TYPE, null);

					for (Element keywordElement : keywordElements) {

						// Add a unique node to selection
						String keyword = Utils
								.getElementContent(keywordElement);
						keywordsNode.addUniqueChild(TreeNode.KEYWORD_TYPE,
								keyword);

						// Remember condition keyword
						conditionKeywords.add(keyword);
					}
				}

				// Add outline node
				processExample(exampleElement, outlineNode, false);
			}
		}

		// Add all condition keywords, without duplicates
		for (String keyword : conditionKeywords) {
			TreeNode.Condition condition = packNode.new Condition(
					TreeNode.Condition.KEYWORD_TYPE);
			condition.setValue(keyword);
			packNode.addCondition(condition);
		}
	}

	public void parseExamples(TreeNode parent) {

		Element packageElement = m_document.getDocumentElement();

		Element examplesElement = Utils.getChildElement(packageElement,
				"examples");
		if (examplesElement != null) {

			List<Element> exampleElements = Utils.getChildElementsList(
					examplesElement, "example");
			for (Element exampleElement : exampleElements) {
				String exampleName = exampleElement.getAttribute("name").trim();

				Element boardElement = Utils.getChildElement(exampleElement,
						"board");

				TreeNode exampleNode = new TreeNode(TreeNode.EXAMPLE_TYPE);
				parent.addChild(exampleNode);

				String boardName;
				boardName = boardElement.getAttribute("name");
				if (boardName.length() > 0) {
					exampleName = exampleName + " (" + boardName + ")";
				}
				exampleNode.setName(exampleName);

				Element descriptionElement = Utils.getChildElement(
						exampleElement, "description");
				String description;
				description = Utils.getElementContent(descriptionElement);
				exampleNode.setDescription(description);

				TreeNode outlineNode = new TreeNode(TreeNode.OUTLINE_TYPE);
				exampleNode.setOutline(outlineNode);

				processExample(exampleElement, outlineNode, true);

			}
		}

	}
}
