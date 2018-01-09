using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class EventProcessor : MonoBehaviour
{
    public GameObject RedCube;
    public GameObject GreenCube;
    public GameObject BlueCube;
    public GameObject ColorCube;

    private System.Object _queueLock = new System.Object();
    List<byte[]> _queuedData = new List<byte[]>();
    List<byte[]> _processingData = new List<byte[]>();

    public void QueueData(byte[] data)
    {
        lock (_queueLock)
        {
            _queuedData.Add(data);
            Debug.Log(_queuedData.Count);
        }
    }
 
    void Update()
    {
        MoveQueuedEventsToExecuting();
        while (_processingData.Count > 0)
        {
            var byteData = _processingData[0];
            _processingData.RemoveAt(0);
            try
            {
                string hexColor = System.Text.Encoding.UTF8.GetString(byteData);
                Color color;
                if (ColorUtility.TryParseHtmlString(hexColor, out color))
                {
                    ColorCube.GetComponent<Renderer>().material.color = color;
                }
            }
            catch (Exception e)
            {
                Debug.Log(e);
            }
        }
    }
    
    private void MoveQueuedEventsToExecuting()
    {
        lock (_queueLock)
        {
            while (_queuedData.Count > 0)
            {
                byte[] data = _queuedData[0];
                _processingData.Add(data);
                _queuedData.RemoveAt(0);
            }
        }
    }
}