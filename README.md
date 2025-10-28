# Anonymization Service

### General Idea
* Anonymizes personal data to ensure GDPR compliance
* Input: Data to be anonymized along with the anonymization configuration
  - json-ld input data or flat json
  - output is in the same former (json-ld input -> json-ld output, flat-json input -> flat-json output)
* The hosted ontology defines the allowed operations for each attribute
  - anonamyization of objects of differnt types is supported (for each type a anonymization strategy must be defined)
* During the anonymization process, an optimizer is created for each attribute individually
* The allowed anonymization operations depend on the attribute type and datatype, as defined in the ontology
* The implemented anonymization methods are described below
* The swagger documentation of the service is accessible via https://anonymizer.go-data.at/swagger-ui/index.html#/

### Anonymization Process
In general, the anonymization process takes place in three steps:
* Fetching of the configuration
* Extracting the configuration form the knowledge graph
* Creation of anonymization operators for each attribute
* Creating anonymized values and adding them to the input model
* Removing the original values for which anonymization was defined
* Constructing output

#### Fetching of the Configuration
At the beginning, the configuration is fetched from the provided URL. The configuration must be stored as a knowledge graph in JSON-LD format. Once the input is fetched, it is validated to ensure that it is valid JSON-LD.

#### Extraction of Configuration
From the knowledge graph, the specific configuration set up for each attribute must be extracted. For each attribute, two pieces of information are important: the expected data type and the intended anonymization type. A SPARQL query is used to find all entities that have AnonymisationDemo as their domain. For those entities, the range attribute defines the data type, and the classification attribute defines the anonymization type.

#### Anonymizer Creation
If the configuration is valid, an anonymizer is instantiated for each attribute. If no implementation is available for the requested anonymization type and the attribute's data type, an exception is returned. The result of this step is a list of anonymizers that can be applied in a loop to anonymize each attribute.

#### Anonymization
To make the anonymizer list applicable, the input data must be restructured. The anonymizer processes data grouped by instance rather than by attribute. Therefore, a list of values is created for each attribute. The corresponding anonymizer is then applied to each list.

#### Removal of original values
For the attribute for which anonymization was applied the original values are removed from the knowledge graph

#### Creation of output
If the output is returend in a knowledge graph format the model is returned as json-ld. If a flat json return value is requested, a flat json represention is created

An anonymizer takes a list of attribute values as input and returns the anonymized values in the same order. Finally, the anonymized attribute lists are transformed back into an instance-oriented schema.The process is visualized below.
![Anonymization_Process](figures/Anonymization_Process.png)

### Anonymization Operations

The service is implemented in a way to enable easy intergration of new anonymization operation.

#### Number of Buckets

For both generalization and randomization, a bucket count (denoted as g) is required. This value is derived from the number of instances k in the dataset and the number of anonymized attributes n. The formula is shown below.

The number of buckets is calculated to ensure at least a 99% probability that no individual in the dataset is uniquely identifiable.

* Line 1 defines the probability that two individuals share the same anonymized values across all attributes.
* Line 2 defines the probability that a given individual is not unique—i.e., at least one other individual has the same anonymized attribute values.
* Line 3 defines the probability that no individual in the dataset is unique.
* In Line 4, the number of buckets is computed by rearranging the formula from Line 3 to determine the required group count that ensures, with at least 99% probability, that all individuals in the dataset are non-unique.
![Bucket_Calculation](figures/Bucket_Calculation.png)

#### Masking

In maksing the attributes is completly hidden. The original value is replaces with the maksing string "****\*". While this ensures full anonymization, no information of the underlying data is kept in the data. This type of anonymization can be applied to every data type. 
Name: "Peter Parker" --> Name: "*****"


#### Generalization

In the generalization the attributes are classified into buckets and the class label is written to the anonymized data set. The number of buckets is defined by the number of instances in the data set:
$$ nrBuckets = \sqrt( numberInstances )$$
Afterwards, the individuals are assigned to buckets based on their value. An approach was chosen in which each bucket constains the same number of values. Thereby, no single buckets are created for outliers making them easily distinguishable from other instances in the anonymized data set. The instances are sorted and then assigned to a bucket based on their position. Based on the values in each a bucket a label for the bucket is created. 

 ![Generalization](figures/Generalization.png)

##### Generalization for Object

eneralization was also implemented for object data, provided that a hierarchical relationship exists between the attributes. These object attributes must be defined in the configuration. The process then iteratively reduces each data point to its corresponding attribute value, starting from the lowest level in the hierarchy.

If the mapping at the current level achieves sufficient anonymization, those attribute values are returned as the anonymized output. Otherwise, the process continues to the next level in the hierarchy. If anonymization still cannot be achieved at the highest level, masking is applied as a fallback.

Currently, the criterion for "sufficient anonymization" at any level is that each group must contain at least three data points.

A common example where this type of anonymization can be applied is addresses. The process is illustrated in the figure below. It first checks whether using the city-level attribute provides enough anonymization. Since there are six groups with only one value each, this level is not sufficient. The process then evaluates the state level, where three groups are formed—but two of them still contain fewer than three values. Finally, the country level is assessed, and since each group contains at least three values, it is used for output.

![address_generalization](figures/address_generalization.png)


#### Randomization

In randomization, a random value is added to each data point. The added value follows a normal distribution, with its spread depending on the number of instances, the distribution of the data, and the number of buckets used. The salt is then the normal distribution multiplied by the distance to the ith closest values, where i is the number of instances per bucket.

https://anonymizer.go-data.at/swagger-ui/index.html#/

<details><summary>Anonymization</summary>

* PUT /api/anonymise

```json
{
  "configurationUrl": "https://soya.ownyourdata.eu/AnonymisationDemo",
  "data": {
    "@context": {
      "oyd": "https://soya.ownyourdata.eu/AnonymisationDemo/"
    },
    "@graph": [
      {
        "@id": "oyd:test1",
        "@type": "oyd:AnonymisationDemo",
        "oyd:latitude": 1234,
        "oyd:longitude": 1234,
        "oyd:geburtsdatum": {
          "@value": "2023-10-01",
          "@type": "xsd:date"
        }
      },
      {
        "@id": "oyd:test2",
        "@type": "oyd:AnonymisationDemo",
        "oyd:geburtsdatum": "1999-10-01"
      },
      {
        "@id": "oyd:test3",
        "@type": "oyd:AnonymisationDemo",
        "oyd:latitude": 12,
        "oyd:longitude": 125,
        "oyd:geburtsdatum": {
          "@value": "2020-10-01",
          "@type": "xsd:date"
        }
      },
      {
        "@id": "oyd:test4",
        "@type": "oyd:AnonymisationDemo",
        "oyd:latitude": 123,
        "oyd:longitude": 12,
        "oyd:geburtsdatum": {
          "@value": "2027-10-01",
          "@type": "xsd:date"
        }
      },
      {
        "@id": "oyd:test5",
        "@type": "oyd:AnonymisationDemo",
        "oyd:latitude": 1234,
        "oyd:longitude": 1234,
        "oyd:geburtsdatum": {
          "@value": "2021-10-01",
          "@type": "xsd:date"
        }
      },
      {
        "@id": "oyd:test6",
        "@type": "oyd:AnonymisationDemo",
        "oyd:latitude": 1,
        "oyd:longitude": 3213
      },
      {
        "@id": "oyd:test7",
        "@type": "oyd:AnonymisationDemo",
        "oyd:latitude": 12,
        "oyd:longitude": 534
      },
      {
        "@id": "oyd:test8",
        "@type": "oyd:AnonymisationDemo",
        "oyd:latitude": 123,
        "oyd:longitude": 124
      },
      {
        "@id": "oyd:test9",
        "@type": "oyd:AnonymisationDemo",
        "oyd:latitude": 123,
        "oyd:longitude": 213
      }
    ]
  }
}
```

* Response
```json
{
  "@graph": [
    {
      "@id": "oyd:test7",
      "oyd:latitude_generalized": {
        "@id": "oyd:latitude_0"
      },
      "oyd:longitude_randomized": {
        "@value": "-1386.0220515164428",
        "@type": "http://www.w3.org/2001/XMLSchema#double"
      },
      "@type": "oyd:AnonymisationDemo"
    },
    {
      "@id": "oyd:latitude_0",
      "http://www.w3.org/2000/01/rdf-schema#max": {
        "@value": "1234.0",
        "@type": "http://www.w3.org/2001/XMLSchema#double"
      },
      "http://www.w3.org/2000/01/rdf-schema#min": {
        "@value": "1.0",
        "@type": "http://www.w3.org/2001/XMLSchema#double"
      },
      "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": "http://ns.ownyourdata.eu/ns/soya-context/generalization"
    },
    {
      "@id": "oyd:test9",
      "oyd:latitude_generalized": {
        "@id": "oyd:latitude_0"
      },
      "oyd:longitude_randomized": {
        "@value": "-248.20239927675317",
        "@type": "http://www.w3.org/2001/XMLSchema#double"
      },
      "@type": "oyd:AnonymisationDemo"
    },
    {
      "@id": "oyd:test2",
      "oyd:geburtsdatum_randomized": {
        "@value": "2052-12-07T00:46:22Z",
        "@type": "http://www.w3.org/2001/XMLSchema#dateTime"
      },
      "@type": "oyd:AnonymisationDemo"
    },
    {
      "@id": "http://ns.ownyourdata.eu/ns/soya-context/kpiObject",
      "http://ns.ownyourdata.eu/ns/soya-context/kAnonymity": {
        "@value": "1",
        "@type": "http://www.w3.org/2001/XMLSchema#long"
      },
      "http://ns.ownyourdata.eu/ns/soya-context/latitudeNumberAttributes": {
        "@value": "1",
        "@type": "http://www.w3.org/2001/XMLSchema#long"
      },
      "http://ns.ownyourdata.eu/ns/soya-context/geburtsdatumNumberAttributes": {
        "@value": "1",
        "@type": "http://www.w3.org/2001/XMLSchema#long"
      },
      "http://ns.ownyourdata.eu/ns/soya-context/longitudeNumberAttributes": {
        "@value": "1",
        "@type": "http://www.w3.org/2001/XMLSchema#long"
      }
    },
    {
      "@id": "oyd:test4",
      "oyd:latitude_generalized": {
        "@id": "oyd:latitude_0"
      },
      "oyd:longitude_randomized": {
        "@value": "-1976.144686999923",
        "@type": "http://www.w3.org/2001/XMLSchema#double"
      },
      "oyd:geburtsdatum_randomized": {
        "@value": "2007-04-09T08:52:41Z",
        "@type": "http://www.w3.org/2001/XMLSchema#dateTime"
      },
      "@type": "oyd:AnonymisationDemo"
    },
    {
      "@id": "oyd:test6",
      "oyd:latitude_generalized": {
        "@id": "oyd:latitude_0"
      },
      "oyd:longitude_randomized": {
        "@value": "3154.139710318729",
        "@type": "http://www.w3.org/2001/XMLSchema#double"
      },
      "@type": "oyd:AnonymisationDemo"
    },
    {
      "@id": "oyd:test8",
      "oyd:latitude_generalized": {
        "@id": "oyd:latitude_0"
      },
      "oyd:longitude_randomized": {
        "@value": "-1419.0737499878442",
        "@type": "http://www.w3.org/2001/XMLSchema#double"
      },
      "@type": "oyd:AnonymisationDemo"
    },
    {
      "@id": "oyd:test1",
      "oyd:latitude_generalized": {
        "@id": "oyd:latitude_0"
      },
      "oyd:longitude_randomized": {
        "@value": "-2118.1293242625266",
        "@type": "http://www.w3.org/2001/XMLSchema#double"
      },
      "oyd:geburtsdatum_randomized": {
        "@value": "2001-11-07T15:46:28Z",
        "@type": "http://www.w3.org/2001/XMLSchema#dateTime"
      },
      "@type": "oyd:AnonymisationDemo"
    },
    {
      "@id": "oyd:test3",
      "oyd:latitude_generalized": {
        "@id": "oyd:latitude_0"
      },
      "oyd:longitude_randomized": {
        "@value": "40.64317785669989",
        "@type": "http://www.w3.org/2001/XMLSchema#double"
      },
      "oyd:geburtsdatum_randomized": {
        "@value": "2001-10-04T08:54:33Z",
        "@type": "http://www.w3.org/2001/XMLSchema#dateTime"
      },
      "@type": "oyd:AnonymisationDemo"
    },
    {
      "@id": "oyd:test5",
      "oyd:latitude_generalized": {
        "@id": "oyd:latitude_0"
      },
      "oyd:longitude_randomized": {
        "@value": "68.85618799322356",
        "@type": "http://www.w3.org/2001/XMLSchema#double"
      },
      "oyd:geburtsdatum_randomized": {
        "@value": "2005-06-26T08:00:50Z",
        "@type": "http://www.w3.org/2001/XMLSchema#dateTime"
      },
      "@type": "oyd:AnonymisationDemo"
    }
  ],
  "@context": {
    "oyd": "https://soya.ownyourdata.eu/AnonymisationDemo/"
  }
}
```

* PUT /api/anonymise/flatjson

```json
{
  "configurationUrl": "https://soya.ownyourdata.eu/AnonymisationDemo",
  "prefix": "https://soya.ownyourdata.eu/AnonymisationDemo/",
  "data": [
    {
      "latitude": 1234,
      "longitude": 1234,
      "test_attribute_not_anonymized": "test"
    },
    {
      "latitude": 123,
      "longitude": 12334
    },
    {
      "latitude": 1221,
      "longitude": 1234
    },
    {
      "latitude": 123,
      "longitude": 12534
    },
    {
      "latitude": 1,
      "longitude": 34
    }
  ]
}
```

* Response
```json
{
  "data": [
    {
      "test_attribute_not_anonymized": "test",
      "longitude_randomized": "-2816.9047756120394",
      "latitude_generalized": {
        "min": "1.0",
        "max": "1234.0"
      }
    },
    {
      "longitude_randomized": "12126.780841320124",
      "latitude_generalized": {
        "min": "1.0",
        "max": "1234.0"
      }
    },
    {
      "longitude_randomized": "-7660.957338690734",
      "latitude_generalized": {
        "min": "1.0",
        "max": "1234.0"
      }
    },
    {
      "longitude_randomized": "5745.373161057553",
      "latitude_generalized": {
        "min": "1.0",
        "max": "1234.0"
      }
    },
    {
      "longitude_randomized": "-15832.739270047625",
      "latitude_generalized": {
        "min": "1.0",
        "max": "1234.0"
      }
    }
  ],
  "kpis": {
    "kAnonymity": "3",
    "latitudeNumberAttributes": "1",
    "longitudeNumberAttributes": "1"
  }
}
```
