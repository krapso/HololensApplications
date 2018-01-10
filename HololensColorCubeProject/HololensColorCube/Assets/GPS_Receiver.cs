using HoloToolkit.Unity.InputModule;
using UnityEngine;
using UnityEngine.XR.WSA.Input;

#if NETFX_CORE
using Windows.Devices.Bluetooth.Advertisement;
using Windows.Storage.Streams;
using System.Runtime.InteropServices.WindowsRuntime;
#endif

public class GPS_Receiver : MonoBehaviour
{
#if NETFX_CORE
    private BluetoothLEAdvertisementWatcher _watcher;
    private BluetoothLEAdvertisementPublisher _publisher;
    public static ushort BEACON_ID = 1775;
    DataWriter _writer;
    private string _color;
#endif
    public EventProcessor eventProcessor;
    private GestureRecognizer gestureRecognizer;

    void Awake()
    {
        eventProcessor.RedCube.GetComponent<Renderer>().material.color = Color.red;
        eventProcessor.GreenCube.GetComponent<Renderer>().material.color = Color.green;
        eventProcessor.BlueCube.GetComponent<Renderer>().material.color = Color.blue;
        gestureRecognizer = new GestureRecognizer();
        gestureRecognizer.SetRecognizableGestures(GestureSettings.Tap);
        gestureRecognizer.Tapped += GestureRecognizerOnTappedEvent;
        gestureRecognizer.StartCapturingGestures();
#if NETFX_CORE
        _color = "#FFFFFF";
        Watcher();
        Publisher();
#endif
    }
#if NETFX_CORE
    private async void Watcher_Received(BluetoothLEAdvertisementWatcher sender, BluetoothLEAdvertisementReceivedEventArgs args)
    {
        ushort identifier = args.Advertisement.ManufacturerData[0].CompanyId;
        byte[] data = args.Advertisement.ManufacturerData[0].Data.ToArray();
        eventProcessor.QueueData(data);
    }

    private async void Watcher()
    {
        _watcher = new BluetoothLEAdvertisementWatcher();
        var manufacturerData = new BluetoothLEManufacturerData
        {
            CompanyId = BEACON_ID
        };
        _watcher.AdvertisementFilter.Advertisement.ManufacturerData.Add(manufacturerData);
        _watcher.Received += Watcher_Received;
        _watcher.Start();
    }

    private async void Publisher()
    {
        _writer = new DataWriter();
        _writer.WriteString(_color);
        _publisher = new BluetoothLEAdvertisementPublisher();
        var manufacturerData = new BluetoothLEManufacturerData
        {
            CompanyId = BEACON_ID,
            Data = _writer.DetachBuffer()
        };
        _publisher.Advertisement.ManufacturerData.Add(manufacturerData);
        _publisher.Start();
    }
#endif

    private void GestureRecognizerOnTappedEvent(TappedEventArgs tappedEventArgs)
    {
        var headPosition = Camera.main.transform.position;
        var gazeDirection = Camera.main.transform.forward;
        RaycastHit hitInfo;
        if (Physics.Raycast(headPosition, gazeDirection, out hitInfo))
        {
            GameObject hitObject = hitInfo.collider.gameObject;
            if (hitObject != null)
            {
#if NETFX_CORE
                if (hitObject.name == eventProcessor.RedCube.name)
                {
                    _publisher.Stop();
                    _color = "#FF0000";
                    Publisher();
                }
                if (hitObject.name == eventProcessor.GreenCube.name)
                {
                    _publisher.Stop();
                    _color = "#00FF00";
                    Publisher();
                }
                if (hitObject.name == eventProcessor.BlueCube.name)
                {
                    _publisher.Stop();
                    _color = "#0000FF";
                    Publisher();
                }
#endif
            }
        }
    }
}